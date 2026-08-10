package hospital.management.backend.dao.patient;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.patient.interfaces.PatientDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.utils.filters.QueryBuilder;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `patients` table (see hospital_schema.sql). */
public class PatientDAOImpl implements PatientDAO {

    private static final String SELECT_COLUMNS =
        "patient_id, first_name, last_name, dob, gender, phone, email, address, created_at, updated_at, deleted_at";

    @Override
    public Patient save(Patient patient) throws Exception {
        UUID id = patient.getPatientId() != null ? UUID.fromString(patient.getPatientId()) : UUID.randomUUID();
        String sql = "INSERT INTO patients (patient_id, first_name, last_name, dob, gender, phone, email, address) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING created_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, patient.getFirstName());
            ps.setString(3, patient.getLastName());
            ps.setDate(4, Date.valueOf(patient.getDob()));
            ps.setString(5, patient.getGender());
            ps.setString(6, patient.getPhone());
            ps.setString(7, patient.getEmail());
            ps.setString(8, patient.getAddress());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    patient.setPatientId(id.toString());
                    patient.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    patient.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save patient: " + e.getMessage(), e);
        }
        return patient;
    }

    @Override
    public Optional<Patient> findById(String patientId) throws Exception {
        return findOneWhere("patient_id = ?", UUID.fromString(patientId));
    }

    @Override
    public Optional<Patient> findByEmail(String email) throws Exception {
        return findOneWhere("email = ?", email);
    }

    private Optional<Patient> findOneWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM patients WHERE " + predicate + " AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up patient: " + e.getMessage(), e);
        }
    }

    @Override
    public PageResult<Patient> findAll(PageRequest request) throws Exception {
        QueryBuilder.SortDir dir = request.getDirection() == PageRequest.SortDirection.DESC
            ? QueryBuilder.SortDir.DESC : QueryBuilder.SortDir.ASC;
        QueryBuilder qb = QueryBuilder.select(SELECT_COLUMNS).from("patients").whereActive();
        String cursorFrag = CursorPagination.whereClause(request, "created_at");
        if (!cursorFrag.isBlank()) qb.and(cursorFrag.trim().replaceFirst("(?i)^AND\\s+", ""));
        String sql = qb.orderBy("created_at", dir).limit(request.getPageSize() + 1).build();
        List<Patient> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list patients: " + e.getMessage(), e);
        }
        return CursorPagination.toResult(rows, request, Patient::getCreatedAt);
    }

    @Override
    public PageResult<Patient> search(String query, PageRequest request) throws Exception {
        boolean hasQuery = query != null && !query.isBlank();
        QueryBuilder.SortDir dir = request.getDirection() == PageRequest.SortDirection.DESC
            ? QueryBuilder.SortDir.DESC : QueryBuilder.SortDir.ASC;
        QueryBuilder qb = QueryBuilder.select(SELECT_COLUMNS).from("patients").whereActive();
        if (hasQuery) qb.whereSearchAny("?", "first_name", "last_name", "CAST(patient_id AS TEXT)");
        String cursorFrag = CursorPagination.whereClause(request, "created_at");
        if (!cursorFrag.isBlank()) qb.and(cursorFrag.trim().replaceFirst("(?i)^AND\\s+", ""));
        String sql = qb.orderBy("created_at", dir).limit(request.getPageSize() + 1).build();
        List<Patient> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (hasQuery) {
                String like = "%" + query.strip() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search patients: " + e.getMessage(), e);
        }
        return CursorPagination.toResult(rows, request, Patient::getCreatedAt);
    }

    @Override
    public Patient update(Patient patient) throws Exception {
        String sql = "UPDATE patients SET first_name = ?, last_name = ?, dob = ?, gender = ?, phone = ?, email = ?, address = ? "
                   + "WHERE patient_id = ? AND deleted_at IS NULL RETURNING updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patient.getFirstName());
            ps.setString(2, patient.getLastName());
            ps.setDate(3, Date.valueOf(patient.getDob()));
            ps.setString(4, patient.getGender());
            ps.setString(5, patient.getPhone());
            ps.setString(6, patient.getEmail());
            ps.setString(7, patient.getAddress());
            ps.setObject(8, UUID.fromString(patient.getPatientId()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ResourceNotFoundException("Patient", patient.getPatientId());
                patient.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update patient: " + e.getMessage(), e);
        }
        return patient;
    }

    @Override
    public void softDelete(String patientId) throws Exception {
        String sql = "UPDATE patients SET deleted_at = CURRENT_TIMESTAMP WHERE patient_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(patientId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("Patient", patientId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete patient: " + e.getMessage(), e);
        }
    }

    private Patient mapRow(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setPatientId(rs.getObject("patient_id", UUID.class).toString());
        p.setFirstName(rs.getString("first_name"));
        p.setLastName(rs.getString("last_name"));
        Date dob = rs.getDate("dob");
        p.setDob(dob != null ? dob.toLocalDate() : null);
        p.setGender(rs.getString("gender"));
        p.setPhone(rs.getString("phone"));
        p.setEmail(rs.getString("email"));
        p.setAddress(rs.getString("address"));
        p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        p.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        p.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return p;
    }
}
