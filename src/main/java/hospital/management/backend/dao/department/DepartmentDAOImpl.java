package hospital.management.backend.dao.department;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.department.interfaces.DepartmentDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `departments` table (see hospital_schema.sql). */
public class DepartmentDAOImpl implements DepartmentDAO {

    private static final String SELECT_COLUMNS =
        "department_id, name, location, phone, created_at, updated_at, deleted_at";

    @Override
    public Department save(Department department) throws Exception {
        UUID id = department.getDepartmentId() != null
                ? UUID.fromString(department.getDepartmentId()) : UUID.randomUUID();
        String sql = "INSERT INTO departments (department_id, name, location, phone) "
                   + "VALUES (?, ?, ?, ?) RETURNING created_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, department.getName());
            ps.setString(3, department.getLocation());
            ps.setString(4, department.getPhone());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    department.setDepartmentId(id.toString());
                    department.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    department.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save department: " + e.getMessage(), e);
        }
        return department;
    }

    @Override
    public Optional<Department> findById(String departmentId) throws Exception {
        return findOneWhere("department_id = ?", UUID.fromString(departmentId));
    }

    @Override
    public Optional<Department> findByName(String name) throws Exception {
        return findOneWhere("name = ?", name);
    }

    private Optional<Department> findOneWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM departments WHERE " + predicate + " AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up department: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Department> findAll() throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM departments WHERE deleted_at IS NULL ORDER BY name";
        List<Department> departments = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) departments.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list departments: " + e.getMessage(), e);
        }
        return departments;
    }

    @Override
    public Department update(Department department) throws Exception {
        String sql = "UPDATE departments SET name = ?, location = ?, phone = ? "
                   + "WHERE department_id = ? AND deleted_at IS NULL RETURNING updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, department.getName());
            ps.setString(2, department.getLocation());
            ps.setString(3, department.getPhone());
            ps.setObject(4, UUID.fromString(department.getDepartmentId()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ResourceNotFoundException("Department", department.getDepartmentId());
                department.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update department: " + e.getMessage(), e);
        }
        return department;
    }

    @Override
    public void softDelete(String departmentId) throws Exception {
        String sql = "UPDATE departments SET deleted_at = CURRENT_TIMESTAMP WHERE department_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(departmentId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("Department", departmentId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete department: " + e.getMessage(), e);
        }
    }

    private Department mapRow(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setDepartmentId(rs.getObject("department_id", UUID.class).toString());
        d.setName(rs.getString("name"));
        d.setLocation(rs.getString("location"));
        d.setPhone(rs.getString("phone"));
        d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        d.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        d.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return d;
    }
}
