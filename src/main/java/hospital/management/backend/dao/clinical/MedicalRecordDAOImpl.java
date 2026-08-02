package hospital.management.backend.dao.clinical;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.clinical.interfaces.MedicalRecordDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.patient.MedicalRecord;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `medical_records` table (see hospital_schema.sql). */
public class MedicalRecordDAOImpl implements MedicalRecordDAO {

    private static final String SELECT_COLUMNS =
        "record_id, appointment_id, diagnosis, symptoms, notes, created_at, updated_at, deleted_at";

    @Override
    public MedicalRecord save(MedicalRecord record) throws Exception {
        UUID id = record.getRecordId() != null ? UUID.fromString(record.getRecordId()) : UUID.randomUUID();
        String sql = "INSERT INTO medical_records (record_id, appointment_id, diagnosis, symptoms, notes) "
                   + "VALUES (?, ?, ?, ?, ?) RETURNING created_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(record.getAppointmentId()));
            ps.setString(3, record.getDiagnosis());
            ps.setString(4, record.getSymptoms());
            ps.setString(5, record.getNotes());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    record.setRecordId(id.toString());
                    record.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    record.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save medical record: " + e.getMessage(), e);
        }
        return record;
    }

    @Override
    public Optional<MedicalRecord> findById(String recordId) throws Exception {
        return findOneWhere("record_id = ?", UUID.fromString(recordId));
    }

    @Override
    public Optional<MedicalRecord> findByAppointmentId(String appointmentId) throws Exception {
        return findOneWhere("appointment_id = ?", UUID.fromString(appointmentId));
    }

    private Optional<MedicalRecord> findOneWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM medical_records WHERE " + predicate + " AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up medical record: " + e.getMessage(), e);
        }
    }

    @Override
    public MedicalRecord update(MedicalRecord record) throws Exception {
        String sql = "UPDATE medical_records SET diagnosis = ?, symptoms = ?, notes = ? "
                   + "WHERE record_id = ? AND deleted_at IS NULL RETURNING updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getDiagnosis());
            ps.setString(2, record.getSymptoms());
            ps.setString(3, record.getNotes());
            ps.setObject(4, UUID.fromString(record.getRecordId()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ResourceNotFoundException("MedicalRecord", record.getRecordId());
                record.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update medical record: " + e.getMessage(), e);
        }
        return record;
    }

    @Override
    public void softDelete(String recordId) throws Exception {
        String sql = "UPDATE medical_records SET deleted_at = CURRENT_TIMESTAMP WHERE record_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(recordId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("MedicalRecord", recordId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete medical record: " + e.getMessage(), e);
        }
    }

    private MedicalRecord mapRow(ResultSet rs) throws SQLException {
        MedicalRecord r = new MedicalRecord();
        r.setRecordId(rs.getObject("record_id", UUID.class).toString());
        r.setAppointmentId(rs.getObject("appointment_id", UUID.class).toString());
        r.setDiagnosis(rs.getString("diagnosis"));
        r.setSymptoms(rs.getString("symptoms"));
        r.setNotes(rs.getString("notes"));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        r.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        r.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return r;
    }
}