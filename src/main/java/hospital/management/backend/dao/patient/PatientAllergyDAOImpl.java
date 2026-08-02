package hospital.management.backend.dao.patient;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.patient.interfaces.PatientAllergyDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.patient.PatientAllergy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `patient_allergies` table (see hospital_schema.sql). */
public class PatientAllergyDAOImpl implements PatientAllergyDAO {

    private static final String SELECT_COLUMNS =
        "allergy_id, patient_id, allergen, reaction, severity, created_at, updated_at, deleted_at";

    @Override
    public PatientAllergy save(PatientAllergy allergy) throws Exception {
        UUID id = allergy.getAllergyId() != null
                ? UUID.fromString(allergy.getAllergyId()) : UUID.randomUUID();
        String sql = "INSERT INTO patient_allergies (allergy_id, patient_id, allergen, reaction, severity) "
                   + "VALUES (?, ?, ?, ?, ?) RETURNING created_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(allergy.getPatientId()));
            ps.setString(3, allergy.getAllergen());
            ps.setString(4, allergy.getReaction());
            ps.setString(5, allergy.getSeverity());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    allergy.setAllergyId(id.toString());
                    allergy.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    allergy.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save patient allergy: " + e.getMessage(), e);
        }
        return allergy;
    }

    @Override
    public Optional<PatientAllergy> findById(String allergyId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM patient_allergies WHERE allergy_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(allergyId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up patient allergy: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PatientAllergy> findByPatientId(String patientId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM patient_allergies "
                   + "WHERE patient_id = ? AND deleted_at IS NULL ORDER BY created_at DESC";
        List<PatientAllergy> allergies = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(patientId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) allergies.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list patient allergies: " + e.getMessage(), e);
        }
        return allergies;
    }

    @Override
    public void softDelete(String allergyId) throws Exception {
        String sql = "UPDATE patient_allergies SET deleted_at = CURRENT_TIMESTAMP "
                   + "WHERE allergy_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(allergyId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("PatientAllergy", allergyId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete patient allergy: " + e.getMessage(), e);
        }
    }

    private PatientAllergy mapRow(ResultSet rs) throws SQLException {
        PatientAllergy a = new PatientAllergy();
        a.setAllergyId(rs.getObject("allergy_id", UUID.class).toString());
        a.setPatientId(rs.getObject("patient_id", UUID.class).toString());
        a.setAllergen(rs.getString("allergen"));
        a.setReaction(rs.getString("reaction"));
        a.setSeverity(rs.getString("severity"));
        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        a.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        a.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return a;
    }
}
