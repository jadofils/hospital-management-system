package hospital.management.backend.dao.auth;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.auth.interfaces.UserRoleDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.model.user.UserRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** JDBC implementation against the `user_roles` join table (see hospital_schema.sql). */
public class UserRoleDAOImpl implements UserRoleDAO {

    @Override
    public void assign(String userId, String roleId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            assign(userId, roleId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to assign role: " + e.getMessage(), e);
        }
    }

    @Override
    public void assign(String userId, String roleId, Connection conn) throws Exception {
        // Composite PK means a prior revoke leaves a row behind — re-assigning
        // just needs to clear revoked_at rather than fail on a duplicate key.
        String sql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) "
                   + "ON CONFLICT (user_id, role_id) DO UPDATE SET revoked_at = NULL, updated_at = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(userId));
            ps.setObject(2, UUID.fromString(roleId));
            ps.executeUpdate();
        }
    }

    @Override
    public void revoke(String userId, String roleId) throws Exception {
        String sql = "UPDATE user_roles SET revoked_at = CURRENT_TIMESTAMP "
                   + "WHERE user_id = ? AND role_id = ? AND revoked_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(userId));
            ps.setObject(2, UUID.fromString(roleId));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to revoke role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UserRole> findByUserId(String userId) throws Exception {
        return findWhere("user_id = ?", UUID.fromString(userId));
    }

    @Override
    public List<UserRole> findByRoleId(String roleId) throws Exception {
        return findWhere("role_id = ?", UUID.fromString(roleId));
    }

    private List<UserRole> findWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT user_id, role_id, assigned_at, updated_at, revoked_at FROM user_roles "
                   + "WHERE " + predicate + " AND revoked_at IS NULL";
        List<UserRole> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up user roles: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public boolean exists(String userId, String roleId) throws Exception {
        String sql = "SELECT 1 FROM user_roles WHERE user_id = ? AND role_id = ? AND revoked_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(userId));
            ps.setObject(2, UUID.fromString(roleId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check role assignment: " + e.getMessage(), e);
        }
    }

    private UserRole mapRow(ResultSet rs) throws SQLException {
        UserRole ur = new UserRole();
        ur.setUserId(rs.getObject("user_id", UUID.class).toString());
        ur.setRoleId(rs.getObject("role_id", UUID.class).toString());
        ur.setAssignedAt(rs.getTimestamp("assigned_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        ur.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp revokedAt = rs.getTimestamp("revoked_at");
        ur.setRevokedAt(revokedAt != null ? revokedAt.toLocalDateTime() : null);
        return ur;
    }
}
