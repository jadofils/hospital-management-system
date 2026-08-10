package hospital.management.backend.dao.patient;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.patient.interfaces.PatientFeedbackDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.patient.PatientFeedback;

import hospital.management.backend.utils.filters.QueryBuilder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PatientFeedbackDAOImpl implements PatientFeedbackDAO {

    private static final String SELECT_COLS =
        "feedback_id, patient_id, appointment_id, submitted_by, rating, comments, " +
        "date_submitted, created_at, updated_at, deleted_at";

    @Override
    public PatientFeedback save(PatientFeedback feedback) throws Exception {
        String sql = "INSERT INTO patient_feedback " +
            "(patient_id, appointment_id, submitted_by, rating, comments, date_submitted) " +
            "VALUES (?, ?, ?, ?, ?, ?) RETURNING feedback_id, created_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableUUID(ps, 1, feedback.getPatientId());
            setNullableUUID(ps, 2, feedback.getAppointmentId());
            setNullableUUID(ps, 3, feedback.getSubmittedBy());
            ps.setInt(4, feedback.getRating());
            ps.setString(5, feedback.getComments());
            ps.setDate(6, feedback.getDateSubmitted() != null
                ? Date.valueOf(feedback.getDateSubmitted()) : Date.valueOf(java.time.LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    feedback.setFeedbackId(rs.getObject("feedback_id", UUID.class).toString());
                    feedback.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    feedback.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
            return feedback;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save patient feedback: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<PatientFeedback> findById(String feedbackId) throws Exception {
        String sql = "SELECT " + SELECT_COLS + " FROM patient_feedback " +
            "WHERE feedback_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(feedbackId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find patient feedback: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PatientFeedback> findAll() throws Exception {
        String sql = QueryBuilder.select(SELECT_COLS)
            .from("patient_feedback")
            .whereActive()
            .orderBy("date_submitted", QueryBuilder.SortDir.DESC)
            .orderBy("created_at", QueryBuilder.SortDir.DESC)
            .build();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<PatientFeedback> result = new ArrayList<>();
            while (rs.next()) result.add(mapRow(rs));
            return result;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list patient feedback: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PatientFeedback> findByPatientId(String patientId) throws Exception {
        String sql = QueryBuilder.select(SELECT_COLS)
            .from("patient_feedback")
            .where("patient_id = ?")
            .whereActive()
            .orderBy("date_submitted", QueryBuilder.SortDir.DESC)
            .orderBy("created_at", QueryBuilder.SortDir.DESC)
            .build();
        return queryList(sql, UUID.fromString(patientId));
    }

    @Override
    public List<PatientFeedback> findByAppointmentId(String appointmentId) throws Exception {
        String sql = QueryBuilder.select(SELECT_COLS)
            .from("patient_feedback")
            .where("appointment_id = ?")
            .whereActive()
            .orderBy("date_submitted", QueryBuilder.SortDir.DESC)
            .orderBy("created_at", QueryBuilder.SortDir.DESC)
            .build();
        return queryList(sql, UUID.fromString(appointmentId));
    }

    @Override
    public void softDelete(String feedbackId) throws Exception {
        String sql = "UPDATE patient_feedback SET deleted_at = CURRENT_TIMESTAMP " +
            "WHERE feedback_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(feedbackId));
            if (ps.executeUpdate() == 0) {
                throw new ResourceNotFoundException("PatientFeedback", feedbackId);
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete patient feedback: " + e.getMessage(), e);
        }
    }

    private List<PatientFeedback> queryList(String sql, Object param) throws Exception {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                List<PatientFeedback> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query patient feedback: " + e.getMessage(), e);
        }
    }

    private PatientFeedback mapRow(ResultSet rs) throws SQLException {
        PatientFeedback f = new PatientFeedback();
        f.setFeedbackId(rs.getObject("feedback_id", UUID.class).toString());
        UUID patientId = rs.getObject("patient_id", UUID.class);
        f.setPatientId(patientId != null ? patientId.toString() : null);
        UUID appointmentId = rs.getObject("appointment_id", UUID.class);
        f.setAppointmentId(appointmentId != null ? appointmentId.toString() : null);
        UUID submittedBy = rs.getObject("submitted_by", UUID.class);
        f.setSubmittedBy(submittedBy != null ? submittedBy.toString() : null);
        f.setRating(rs.getInt("rating"));
        f.setComments(rs.getString("comments"));
        Date ds = rs.getDate("date_submitted");
        f.setDateSubmitted(ds != null ? ds.toLocalDate() : null);
        Timestamp ca = rs.getTimestamp("created_at");
        f.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        Timestamp ua = rs.getTimestamp("updated_at");
        f.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);
        Timestamp da = rs.getTimestamp("deleted_at");
        f.setDeletedAt(da != null ? da.toLocalDateTime() : null);
        return f;
    }

    private void setNullableUUID(PreparedStatement ps, int index, String value) throws SQLException {
        if (value != null && !value.isBlank()) {
            ps.setObject(index, UUID.fromString(value));
        } else {
            ps.setNull(index, Types.OTHER);
        }
    }
}