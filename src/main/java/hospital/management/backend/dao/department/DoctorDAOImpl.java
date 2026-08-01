package hospital.management.backend.dao.department;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.department.interfaces.DoctorDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `doctors` table (see hospital_schema.sql). */
public class DoctorDAOImpl implements DoctorDAO {

    private static final String SELECT_COLUMNS =
        "doctor_id, department_id, first_name, last_name, specialization, phone, email, created_at, updated_at, deleted_at";

    @Override
    public Doctor save(Doctor doctor) throws Exception {
        UUID id = doctor.getDoctorId() != null ? UUID.fromString(doctor.getDoctorId()) : UUID.randomUUID();
        String sql = "INSERT INTO doctors (doctor_id, department_id, first_name, last_name, specialization, phone, email) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING created_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            if (doctor.getDepartmentId() != null) {
                ps.setObject(2, UUID.fromString(doctor.getDepartmentId()));
            } else {
                ps.setNull(2, Types.OTHER);
            }
            ps.setString(3, doctor.getFirstName());
            ps.setString(4, doctor.getLastName());
            ps.setString(5, doctor.getSpecialization());
            ps.setString(6, doctor.getPhone());
            ps.setString(7, doctor.getEmail());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    doctor.setDoctorId(id.toString());
                    doctor.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    doctor.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save doctor: " + e.getMessage(), e);
        }
        return doctor;
    }

    @Override
    public Optional<Doctor> findById(String doctorId) throws Exception {
        return findOneWhere("doctor_id = ?", UUID.fromString(doctorId));
    }

    @Override
    public Optional<Doctor> findByEmail(String email) throws Exception {
        return findOneWhere("email = ?", email);
    }

    private Optional<Doctor> findOneWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM doctors WHERE " + predicate + " AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up doctor: " + e.getMessage(), e);
        }
    }

    @Override
    public PageResult<Doctor> findAll(PageRequest request) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM doctors WHERE deleted_at IS NULL "
                   + CursorPagination.whereClause(request, "created_at")
                   + CursorPagination.orderClause(request, "created_at")
                   + "LIMIT ?";
        List<Doctor> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, request.getPageSize() + 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list doctors: " + e.getMessage(), e);
        }
        return CursorPagination.toResult(rows, request, Doctor::getCreatedAt);
    }

    @Override
    public List<Doctor> findByDepartmentId(String departmentId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM doctors WHERE department_id = ? AND deleted_at IS NULL ORDER BY last_name, first_name";
        List<Doctor> doctors = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(departmentId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) doctors.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list doctors by department: " + e.getMessage(), e);
        }
        return doctors;
    }

    @Override
    public Doctor update(Doctor doctor) throws Exception {
        String sql = "UPDATE doctors SET department_id = ?, first_name = ?, last_name = ?, specialization = ?, phone = ?, email = ? "
                   + "WHERE doctor_id = ? AND deleted_at IS NULL RETURNING updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (doctor.getDepartmentId() != null) {
                ps.setObject(1, UUID.fromString(doctor.getDepartmentId()));
            } else {
                ps.setNull(1, Types.OTHER);
            }
            ps.setString(2, doctor.getFirstName());
            ps.setString(3, doctor.getLastName());
            ps.setString(4, doctor.getSpecialization());
            ps.setString(5, doctor.getPhone());
            ps.setString(6, doctor.getEmail());
            ps.setObject(7, UUID.fromString(doctor.getDoctorId()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ResourceNotFoundException("Doctor", doctor.getDoctorId());
                doctor.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update doctor: " + e.getMessage(), e);
        }
        return doctor;
    }

    @Override
    public void softDelete(String doctorId) throws Exception {
        String sql = "UPDATE doctors SET deleted_at = CURRENT_TIMESTAMP WHERE doctor_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(doctorId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("Doctor", doctorId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete doctor: " + e.getMessage(), e);
        }
    }

    private Doctor mapRow(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setDoctorId(rs.getObject("doctor_id", UUID.class).toString());
        UUID deptId = rs.getObject("department_id", UUID.class);
        d.setDepartmentId(deptId != null ? deptId.toString() : null);
        d.setFirstName(rs.getString("first_name"));
        d.setLastName(rs.getString("last_name"));
        d.setSpecialization(rs.getString("specialization"));
        d.setPhone(rs.getString("phone"));
        d.setEmail(rs.getString("email"));
        d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        d.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        d.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return d;
    }
}
