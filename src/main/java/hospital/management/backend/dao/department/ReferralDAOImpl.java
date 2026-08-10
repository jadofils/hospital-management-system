package hospital.management.backend.dao.department;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.department.interfaces.ReferralDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Referral;

import hospital.management.backend.utils.filters.QueryBuilder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `referrals` table (see hospital_schema.sql). */
public class ReferralDAOImpl implements ReferralDAO {

    private static final String SELECT_COLUMNS =
        "referral_id, appointment_id, referring_doctor_id, referred_to_doctor_id, reason, status, created_at, updated_at, deleted_at";

    @Override
    public Referral save(Referral referral) throws Exception {
        UUID id = referral.getReferralId() != null ? UUID.fromString(referral.getReferralId()) : UUID.randomUUID();
        String sql = "INSERT INTO referrals (referral_id, appointment_id, referring_doctor_id, referred_to_doctor_id, reason, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?) RETURNING created_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(referral.getAppointmentId()));
            ps.setObject(3, UUID.fromString(referral.getReferringDoctorId()));
            ps.setObject(4, UUID.fromString(referral.getReferredToDoctorId()));
            ps.setString(5, referral.getReason());
            ps.setString(6, referral.getStatus() != null ? referral.getStatus() : "pending");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    referral.setReferralId(id.toString());
                    if (referral.getStatus() == null) referral.setStatus("pending");
                    referral.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    referral.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save referral: " + e.getMessage(), e);
        }
        return referral;
    }

    @Override
    public Optional<Referral> findById(String referralId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM referrals WHERE referral_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(referralId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up referral: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Referral> findByAppointmentId(String appointmentId) throws Exception {
        return findAllWhere("appointment_id = ?", UUID.fromString(appointmentId));
    }

    @Override
    public List<Referral> findByReferringDoctorId(String doctorId) throws Exception {
        return findAllWhere("referring_doctor_id = ?", UUID.fromString(doctorId));
    }

    private List<Referral> findAllWhere(String predicate, Object param) throws Exception {
        String sql = QueryBuilder.select(SELECT_COLUMNS)
            .from("referrals")
            .where(predicate)
            .whereActive()
            .orderBy("created_at", QueryBuilder.SortDir.DESC)
            .build();
        List<Referral> referrals = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) referrals.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list referrals: " + e.getMessage(), e);
        }
        return referrals;
    }

    @Override
    public Referral updateStatus(String referralId, String status) throws Exception {
        String sql = "UPDATE referrals SET status = ? WHERE referral_id = ? AND deleted_at IS NULL RETURNING " + SELECT_COLUMNS;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setObject(2, UUID.fromString(referralId));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ResourceNotFoundException("Referral", referralId);
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update referral status: " + e.getMessage(), e);
        }
    }

    @Override
    public void softDelete(String referralId) throws Exception {
        String sql = "UPDATE referrals SET deleted_at = CURRENT_TIMESTAMP WHERE referral_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(referralId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("Referral", referralId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete referral: " + e.getMessage(), e);
        }
    }

    private Referral mapRow(ResultSet rs) throws SQLException {
        Referral r = new Referral();
        r.setReferralId(rs.getObject("referral_id", UUID.class).toString());
        r.setAppointmentId(rs.getObject("appointment_id", UUID.class).toString());
        r.setReferringDoctorId(rs.getObject("referring_doctor_id", UUID.class).toString());
        r.setReferredToDoctorId(rs.getObject("referred_to_doctor_id", UUID.class).toString());
        r.setReason(rs.getString("reason"));
        r.setStatus(rs.getString("status"));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        r.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        r.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return r;
    }
}
