package hospital.management.backend.dao.auth;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.auth.interfaces.UserDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.user.User;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `users` table (see hospital_schema.sql). */
public class UserDAOImpl implements UserDAO {

    private static final String SELECT_COLUMNS =
        "user_id, doctor_id, username, password_hash, email, is_active, created_at, updated_at, deleted_at";

    @Override
    public User save(User user) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return save(user, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save user: " + e.getMessage(), e);
        }
    }

    @Override
    public User save(User user, Connection conn) throws Exception {
        UUID id = user.getUserId() != null ? UUID.fromString(user.getUserId()) : UUID.randomUUID();
        String sql = "INSERT INTO users (user_id, doctor_id, username, password_hash, email, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?) RETURNING created_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            if (user.getDoctorId() != null) {
                ps.setObject(2, UUID.fromString(user.getDoctorId()));
            } else {
                ps.setNull(2, Types.OTHER);
            }
            ps.setString(3, user.getUsername());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getEmail());
            ps.setBoolean(6, user.getIsActive() != null ? user.getIsActive() : true);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setUserId(id.toString());
                    user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return user;
    }

    @Override
    public Optional<User> findById(String userId) throws Exception {
        return findOneWhere("user_id = ?", UUID.fromString(userId));
    }

    @Override
    public Optional<User> findByUsername(String username) throws Exception {
        return findOneWhere("username = ?", username);
    }

    @Override
    public Optional<User> findByEmail(String email) throws Exception {
        return findOneWhere("email = ?", email);
    }

    @Override
    public Optional<User> findByDoctorId(String doctorId) throws Exception {
        return findOneWhere("doctor_id = ?", UUID.fromString(doctorId));
    }

    private Optional<User> findOneWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM users WHERE " + predicate + " AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up user: " + e.getMessage(), e);
        }
    }

    @Override
    public PageResult<User> findAll(PageRequest request) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM users WHERE deleted_at IS NULL "
                   + CursorPagination.whereClause(request, "created_at")
                   + CursorPagination.orderClause(request, "created_at")
                   + "LIMIT ?";
        List<User> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, request.getPageSize() + 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list users: " + e.getMessage(), e);
        }
        return CursorPagination.toResult(rows, request, User::getCreatedAt);
    }

    @Override
    public User update(User user) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return update(user, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update user: " + e.getMessage(), e);
        }
    }

    @Override
    public User update(User user, Connection conn) throws Exception {
        String sql = "UPDATE users SET email = ?, is_active = ? "
                   + "WHERE user_id = ? AND deleted_at IS NULL RETURNING updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setBoolean(2, user.getIsActive() != null ? user.getIsActive() : true);
            ps.setObject(3, UUID.fromString(user.getUserId()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ResourceNotFoundException("User", user.getUserId());
                user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
        }
        return user;
    }

    @Override
    public void updatePasswordHash(String userId, String newPasswordHash) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            updatePasswordHash(userId, newPasswordHash, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update password: " + e.getMessage(), e);
        }
    }

    @Override
    public void updatePasswordHash(String userId, String newPasswordHash, Connection conn) throws Exception {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setObject(2, UUID.fromString(userId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("User", userId);
        }
    }

    @Override
    public void softDelete(String userId) throws Exception {
        String sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE user_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(userId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("User", userId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByUsername(String username) throws Exception {
        return existsWhere("username = ?", username);
    }

    @Override
    public boolean existsByEmail(String email) throws Exception {
        return existsWhere("email = ?", email);
    }

    private boolean existsWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT 1 FROM users WHERE " + predicate + " AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check user existence: " + e.getMessage(), e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getObject("user_id", UUID.class).toString());
        UUID doctorId = rs.getObject("doctor_id", UUID.class);
        u.setDoctorId(doctorId != null ? doctorId.toString() : null);
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setEmail(rs.getString("email"));
        u.setIsActive(rs.getBoolean("is_active"));
        u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        u.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        u.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return u;
    }
}
