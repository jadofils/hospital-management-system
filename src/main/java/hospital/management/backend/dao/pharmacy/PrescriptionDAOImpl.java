package hospital.management.backend.dao.pharmacy;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.pharmacy.interfaces.PrescriptionDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.pharmacy.Prescription;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `prescriptions` table (see hospital_schema.sql). */
public class PrescriptionDAOImpl implements PrescriptionDAO {

    private static final String SELECT_COLUMNS =
        "prescription_id, appointment_id, date_issued, created_at, updated_at, deleted_at";

    @Override
    public Prescription save(Prescription prescription) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return save(prescription, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save prescription: " + e.getMessage(), e);
        }
    }

    @Override
    public Prescription save(Prescription prescription, Connection conn) throws Exception {
        UUID id = prescription.getPrescriptionId() != null
                ? UUID.fromString(prescription.getPrescriptionId()) : UUID.randomUUID();
        String sql = "INSERT INTO prescriptions (prescription_id, appointment_id, date_issued) "
                   + "VALUES (?, ?, ?) RETURNING created_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(prescription.getAppointmentId()));
            ps.setDate(3, Date.valueOf(prescription.getDateIssued()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    prescription.setPrescriptionId(id.toString());
                    prescription.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    prescription.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return prescription;
    }

    @Override
    public Optional<Prescription> findById(String prescriptionId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM prescriptions WHERE prescription_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(prescriptionId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up prescription: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Prescription> findByAppointmentId(String appointmentId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM prescriptions WHERE appointment_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(appointmentId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up prescription by appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Prescription> findByPatientId(String patientId) throws Exception {
        String sql = "SELECT p.prescription_id, p.appointment_id, p.date_issued, p.created_at, p.updated_at, p.deleted_at "
                   + "FROM prescriptions p "
                   + "JOIN appointments a ON a.appointment_id = p.appointment_id "
                   + "WHERE a.patient_id = ? AND p.deleted_at IS NULL "
                   + "ORDER BY p.date_issued DESC, p.created_at DESC";
        List<Prescription> prescriptions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(patientId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) prescriptions.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list prescriptions by patient: " + e.getMessage(), e);
        }
        return prescriptions;
    }

    @Override
    public void softDelete(String prescriptionId) throws Exception {
        String sql = "UPDATE prescriptions SET deleted_at = CURRENT_TIMESTAMP WHERE prescription_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(prescriptionId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("Prescription", prescriptionId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete prescription: " + e.getMessage(), e);
        }
    }

    private Prescription mapRow(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.setPrescriptionId(rs.getObject("prescription_id", UUID.class).toString());
        p.setAppointmentId(rs.getObject("appointment_id", UUID.class).toString());
        Date dateIssued = rs.getDate("date_issued");
        p.setDateIssued(dateIssued != null ? dateIssued.toLocalDate() : null);
        p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        p.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        p.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return p;
    }
}
