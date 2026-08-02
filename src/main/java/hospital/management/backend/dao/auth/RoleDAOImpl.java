package hospital.management.backend.dao.auth;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.auth.interfaces.RoleDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.user.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `roles` table (see hospital_schema.sql). */
public class RoleDAOImpl implements RoleDAO {

    private static final String SELECT_COLUMNS = "role_id, role_name, created_at, updated_at, deleted_at";

    @Override
    public Role save(Role role) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return save(role, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save role: " + e.getMessage(), e);
        }
    }

    @Override
    public Role save(Role role, Connection conn) throws Exception {
        UUID id = role.getRoleId() != null ? UUID.fromString(role.getRoleId()) : UUID.randomUUID();
        String sql = "INSERT INTO roles (role_id, role_name) VALUES (?, ?) RETURNING created_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, role.getRoleName());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    role.setRoleId(id.toString());
                    role.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    role.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return role;
    }

    @Override
    public Optional<Role> findById(String roleId) throws Exception {
        return findOneWhere("role_id = ?", UUID.fromString(roleId));
    }

    @Override
    public Optional<Role> findByName(String roleName) throws Exception {
        return findOneWhere("role_name = ?", roleName);
    }

    private Optional<Role> findOneWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM roles WHERE " + predicate + " AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Role> findAll() throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM roles WHERE deleted_at IS NULL ORDER BY role_name";
        List<Role> roles = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) roles.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list roles: " + e.getMessage(), e);
        }
        return roles;
    }

    @Override
    public void softDelete(String roleId) throws Exception {
        String sql = "UPDATE roles SET deleted_at = CURRENT_TIMESTAMP WHERE role_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(roleId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("Role", roleId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete role: " + e.getMessage(), e);
        }
    }

    private Role mapRow(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setRoleId(rs.getObject("role_id", UUID.class).toString());
        role.setRoleName(rs.getString("role_name"));
        role.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        role.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        role.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return role;
    }
}
