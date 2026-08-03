package hospital.management.backend.dao.patient;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.patient.interfaces.PatientFeedbackDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.patient.PatientFeedback;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `patient_feedback` table (see hospital_schema.sql). */
public class PatientFeedbackDAOImpl implements PatientFeedbackDAO {

    private static final String SELECT_COLUMNS =
        "feedback_id, patient_id, appointment_id, rating, comments, date_submitted, created_at, updated_at, deleted_at";

    @Override
    public PatientFeedback save(PatientFeedback feedback) throws Exception {
        UUID id = feedback.getFeedbackId() != null
                ? UUID.fromString(feedback.getFeedbackId()) : UUID.randomUUID();
        // date_submitted has a NOT NULL DEFAULT CURRENT_DATE — when the caller
        // doesn't supply one, COALESCE(?, CURRENT_DATE) lets the DB default apply
        // instead of failing the NOT NULL constraint with an explicit NULL.
        String sql = "INSERT INTO patient_feedback "
                   + "(feedback_id, patient_id, appointment_id, rating, comments, date_submitted) "
                   + "VALUES (?, ?, ?, ?, ?, COALESCE(?, CURRENT_DATE)) "
                   + "RETURNING date_submitted, created_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(feedback.getPatientId()));
            if (feedback.getAppointmentId() != null) {
                ps.setObject(3, UUID.fromString(feedback.getAppointmentId()));
            } else {
                ps.setNull(3, Types.OTHER);
            }
            ps.setInt(4, feedback.getRating());
            ps.setString(5, feedback.getComments());
            if (feedback.getDateSubmitted() != null) {
                ps.setDate(6, Date.valueOf(feedback.getDateSubmitted()));
            } else {
                ps.setNull(6, Types.DATE);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    feedback.setFeedbackId(id.toString());
                    feedback.setDateSubmitted(rs.getDate("date_submitted").toLocalDate());
                    feedback.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    feedback.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save patient feedback: " + e.getMessage(), e);
        }
        return feedback;
    }

    @Override
    public Optional<PatientFeedback> findById(String feedbackId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM patient_feedback WHERE feedback_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(feedbackId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up patient feedback: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PatientFeedback> findAll() throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM patient_feedback WHERE deleted_at IS NULL ORDER BY date_submitted DESC, created_at DESC";
        List<PatientFeedback> feedbacks = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) feedbacks.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list patient feedback: " + e.getMessage(), e);
        }
        return feedbacks;
    }

    @Override
    public List<PatientFeedback> findByPatientId(String patientId) throws Exception {
        return findAllWhere("patient_id = ?", UUID.fromString(patientId));
    }

    @Override
    public List<PatientFeedback> findByAppointmentId(String appointmentId) throws Exception {
        return findAllWhere("appointment_id = ?", UUID.fromString(appointmentId));
    }

    private List<PatientFeedback> findAllWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM patient_feedback "
                   + "WHERE " + predicate + " AND deleted_at IS NULL ORDER BY date_submitted DESC, created_at DESC";
        List<PatientFeedback> feedbacks = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) feedbacks.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list patient feedback: " + e.getMessage(), e);
        }
        return feedbacks;
    }

    @Override
    public void softDelete(String feedbackId) throws Exception {
        String sql = "UPDATE patient_feedback SET deleted_at = CURRENT_TIMESTAMP "
                   + "WHERE feedback_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(feedbackId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("PatientFeedback", feedbackId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete patient feedback: " + e.getMessage(), e);
        }
    }

    private PatientFeedback mapRow(ResultSet rs) throws SQLException {
        PatientFeedback f = new PatientFeedback();
        f.setFeedbackId(rs.getObject("feedback_id", UUID.class).toString());
        f.setPatientId(rs.getObject("patient_id", UUID.class).toString());
        UUID appointmentId = rs.getObject("appointment_id", UUID.class);
        f.setAppointmentId(appointmentId != null ? appointmentId.toString() : null);
        f.setRating(rs.getInt("rating"));
        f.setComments(rs.getString("comments"));
        Date dateSubmitted = rs.getDate("date_submitted");
        f.setDateSubmitted(dateSubmitted != null ? dateSubmitted.toLocalDate() : null);
        f.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        f.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        f.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return f;
    }
}
