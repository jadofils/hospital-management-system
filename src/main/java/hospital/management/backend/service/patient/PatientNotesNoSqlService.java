package hospital.management.backend.service.patient;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dto.patient.PatientNoteDTO;
import hospital.management.backend.exceptions.DatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists and retrieves free-text patient notes from the PostgreSQL patient_notes table.
 */
public class PatientNotesNoSqlService {

    private static final String SELECT_COLS =
        "note_id, patient_id, appointment_id, author_user_id, author_role, note_text, source, created_at";

    public String saveNote(String patientId, String appointmentId, String authorUserId,
                           String authorRole, String noteText) throws Exception {
        if (noteText == null || noteText.trim().isEmpty()) return null;
        String sql = "INSERT INTO patient_notes " +
            "(patient_id, appointment_id, author_user_id, author_role, note_text, source) " +
            "VALUES (?, ?, ?, ?, ?, 'medical_records') RETURNING note_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(patientId));
            setNullableUUID(ps, 2, appointmentId);
            setNullableUUID(ps, 3, authorUserId);
            ps.setString(4, authorRole);
            ps.setString(5, noteText.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getObject("note_id", UUID.class).toString();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save patient note: " + e.getMessage(), e);
        }
        return null;
    }

    public List<PatientNoteDTO> findByPatientId(String patientId) throws Exception {
        String sql = "SELECT " + SELECT_COLS + " FROM patient_notes " +
            "WHERE patient_id = ? AND deleted_at IS NULL ORDER BY created_at DESC";
        return query(sql, UUID.fromString(patientId));
    }

    public List<PatientNoteDTO> findByAppointmentId(String appointmentId) throws Exception {
        String sql = "SELECT " + SELECT_COLS + " FROM patient_notes " +
            "WHERE appointment_id = ? AND deleted_at IS NULL ORDER BY created_at DESC";
        return query(sql, UUID.fromString(appointmentId));
    }

    private List<PatientNoteDTO> query(String sql, UUID param) throws Exception {
        List<PatientNoteDTO> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query patient notes: " + e.getMessage(), e);
        }
        return result;
    }

    private PatientNoteDTO mapRow(ResultSet rs) throws SQLException {
        PatientNoteDTO dto = new PatientNoteDTO();
        dto.setNoteId(rs.getObject("note_id", UUID.class).toString());
        UUID pid = rs.getObject("patient_id", UUID.class);
        dto.setPatientId(pid != null ? pid.toString() : null);
        UUID aid = rs.getObject("appointment_id", UUID.class);
        dto.setAppointmentId(aid != null ? aid.toString() : null);
        UUID uid = rs.getObject("author_user_id", UUID.class);
        dto.setAuthorUserId(uid != null ? uid.toString() : null);
        dto.setAuthorRole(rs.getString("author_role"));
        dto.setNoteText(rs.getString("note_text"));
        dto.setSource(rs.getString("source"));
        Timestamp ca = rs.getTimestamp("created_at");
        dto.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        return dto;
    }

    private void setNullableUUID(PreparedStatement ps, int index, String value) throws SQLException {
        if (value != null && !value.isBlank()) {
            ps.setObject(index, UUID.fromString(value));
        } else {
            ps.setNull(index, Types.OTHER);
        }
    }
}