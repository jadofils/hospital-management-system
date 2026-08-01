package hospital.management.backend.dao.auth;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.auth.interfaces.PermissionDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.user.Permission;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `permissions` table (see hospital_schema.sql). */
public class PermissionDAOImpl implements PermissionDAO {

    private static final String SELECT_COLUMNS =
        "permission_id, resource, action, created_at, updated_at, deleted_at";

    @Override
    public Permission save(Permission permission) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return save(permission, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save permission: " + e.getMessage(), e);
        }
    }

    @Override
    public Permission save(Permission permission, Connection conn) throws Exception {
        UUID id = permission.getPermissionId() != null
                ? UUID.fromString(permission.getPermissionId()) : UUID.randomUUID();
        String sql = "INSERT INTO permissions (permission_id, resource, action) VALUES (?, ?, ?) "
                   + "RETURNING created_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, permission.getResource());
            ps.setString(3, permission.getAction());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    permission.setPermissionId(id.toString());
                    permission.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    permission.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return permission;
    }

    @Override
    public Optional<Permission> findById(String permissionId) throws Exception {
        return findOneWhere("permission_id = ?", UUID.fromString(permissionId));
    }

    @Override
    public Optional<Permission> findByResourceAndAction(String resource, String action) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM permissions "
                   + "WHERE resource = ? AND action = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resource);
            ps.setString(2, action);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up permission: " + e.getMessage(), e);
        }
    }

    private Optional<Permission> findOneWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM permissions WHERE " + predicate + " AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up permission: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Permission> findAll() throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM permissions WHERE deleted_at IS NULL "
                   + "ORDER BY resource, action";
        List<Permission> permissions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) permissions.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list permissions: " + e.getMessage(), e);
        }
        return permissions;
    }

    @Override
    public void softDelete(String permissionId) throws Exception {
        String sql = "UPDATE permissions SET deleted_at = CURRENT_TIMESTAMP WHERE permission_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(permissionId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("Permission", permissionId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete permission: " + e.getMessage(), e);
        }
    }

    private Permission mapRow(ResultSet rs) throws SQLException {
        Permission p = new Permission();
        p.setPermissionId(rs.getObject("permission_id", UUID.class).toString());
        p.setResource(rs.getString("resource"));
        p.setAction(rs.getString("action"));
        p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        p.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        p.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return p;
    }
}
