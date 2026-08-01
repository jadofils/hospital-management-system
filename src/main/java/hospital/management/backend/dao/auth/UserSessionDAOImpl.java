package hospital.management.backend.dao.auth;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.auth.interfaces.UserSessionDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.model.user.UserSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `user_sessions` table (see hospital_schema.sql). */
public class UserSessionDAOImpl implements UserSessionDAO {

    private static final String SELECT_COLUMNS =
        "session_id, user_id, login_at, logout_at, expires_at, ip_address, user_agent, is_active, updated_at";

    @Override
    public UserSession save(UserSession session) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return save(session, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save session: " + e.getMessage(), e);
        }
    }

    @Override
    public UserSession save(UserSession session, Connection conn) throws Exception {
        UUID id = session.getSessionId() != null ? UUID.fromString(session.getSessionId()) : UUID.randomUUID();
        String sql = "INSERT INTO user_sessions (session_id, user_id, expires_at, ip_address, user_agent, is_active) "
                   + "VALUES (?, ?, ?, ?, ?, ?) RETURNING login_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(session.getUserId()));
            ps.setTimestamp(3, Timestamp.valueOf(session.getExpiresAt()));
            ps.setString(4, session.getIpAddress());
            ps.setString(5, session.getUserAgent());
            ps.setBoolean(6, session.isIsActive() != null ? session.isIsActive() : true);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    session.setSessionId(id.toString());
                    session.setLoginAt(rs.getTimestamp("login_at").toLocalDateTime());
                    session.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return session;
    }

    @Override
    public Optional<UserSession> findById(String sessionId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM user_sessions WHERE session_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(sessionId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up session: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UserSession> findActiveByUserId(String userId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM user_sessions "
                   + "WHERE user_id = ? AND is_active = TRUE ORDER BY login_at DESC";
        List<UserSession> sessions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(userId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) sessions.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list active sessions: " + e.getMessage(), e);
        }
        return sessions;
    }

    @Override
    public void deactivate(String sessionId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            deactivate(sessionId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to close session: " + e.getMessage(), e);
        }
    }

    @Override
    public void deactivate(String sessionId, Connection conn) throws Exception {
        String sql = "UPDATE user_sessions SET is_active = FALSE, logout_at = CURRENT_TIMESTAMP "
                   + "WHERE session_id = ? AND is_active = TRUE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(sessionId));
            ps.executeUpdate();
        }
    }

    @Override
    public void deactivateAll(String userId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            deactivateAll(userId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to close sessions: " + e.getMessage(), e);
        }
    }

    @Override
    public void deactivateAll(String userId, Connection conn) throws Exception {
        String sql = "UPDATE user_sessions SET is_active = FALSE, logout_at = CURRENT_TIMESTAMP "
                   + "WHERE user_id = ? AND is_active = TRUE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(userId));
            ps.executeUpdate();
        }
    }

    private UserSession mapRow(ResultSet rs) throws SQLException {
        UserSession s = new UserSession();
        s.setSessionId(rs.getObject("session_id", UUID.class).toString());
        s.setUserId(rs.getObject("user_id", UUID.class).toString());
        s.setLoginAt(rs.getTimestamp("login_at").toLocalDateTime());
        Timestamp logoutAt = rs.getTimestamp("logout_at");
        s.setLogoutAt(logoutAt != null ? logoutAt.toLocalDateTime() : null);
        s.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        s.setIpAddress(rs.getString("ip_address"));
        s.setUserAgent(rs.getString("user_agent"));
        s.setIsActive(rs.getBoolean("is_active"));
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        s.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        return s;
    }
}
