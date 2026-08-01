package hospital.management.backend.dao.auth;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.auth.interfaces.RolePermissionDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.model.user.RolePermission;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** JDBC implementation against the `role_permissions` join table (see hospital_schema.sql). */
public class RolePermissionDAOImpl implements RolePermissionDAO {

    @Override
    public void assign(String roleId, String permissionId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            assign(roleId, permissionId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to assign permission: " + e.getMessage(), e);
        }
    }

    @Override
    public void assign(String roleId, String permissionId, Connection conn) throws Exception {
        // Composite PK means a prior revoke leaves a row behind — re-assigning
        // just needs to clear deleted_at rather than fail on a duplicate key.
        String sql = "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?) "
                   + "ON CONFLICT (role_id, permission_id) DO UPDATE SET deleted_at = NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(roleId));
            ps.setObject(2, UUID.fromString(permissionId));
            ps.executeUpdate();
        }
    }

    @Override
    public void revoke(String roleId, String permissionId) throws Exception {
        String sql = "UPDATE role_permissions SET deleted_at = CURRENT_TIMESTAMP "
                   + "WHERE role_id = ? AND permission_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(roleId));
            ps.setObject(2, UUID.fromString(permissionId));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to revoke permission: " + e.getMessage(), e);
        }
    }

    @Override
    public List<RolePermission> findByRoleId(String roleId) throws Exception {
        return findWhere("role_id = ?", UUID.fromString(roleId));
    }

    @Override
    public List<RolePermission> findByPermissionId(String permissionId) throws Exception {
        return findWhere("permission_id = ?", UUID.fromString(permissionId));
    }

    private List<RolePermission> findWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT role_id, permission_id, created_at, deleted_at FROM role_permissions "
                   + "WHERE " + predicate + " AND deleted_at IS NULL";
        List<RolePermission> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up role permissions: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public boolean exists(String roleId, String permissionId) throws Exception {
        String sql = "SELECT 1 FROM role_permissions WHERE role_id = ? AND permission_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(roleId));
            ps.setObject(2, UUID.fromString(permissionId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check permission assignment: " + e.getMessage(), e);
        }
    }

    private RolePermission mapRow(ResultSet rs) throws SQLException {
        RolePermission rp = new RolePermission();
        rp.setRoleId(rs.getObject("role_id", UUID.class).toString());
        rp.setPermissionId(rs.getObject("permission_id", UUID.class).toString());
        rp.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        rp.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return rp;
    }
}
