package hospital.management.backend.dao.pharmacy;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.pharmacy.interfaces.MedicationDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.pharmacy.Medication;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `medications` table (see hospital_schema.sql). */
public class MedicationDAOImpl implements MedicationDAO {

    private static final String SELECT_COLUMNS =
        "medication_id, name, generic_name, form, unit_price, created_at, updated_at, deleted_at";

    @Override
    public Medication save(Medication medication) throws Exception {
        UUID id = medication.getMedicationId() != null
                ? UUID.fromString(medication.getMedicationId()) : UUID.randomUUID();
        String sql = "INSERT INTO medications (medication_id, name, generic_name, form, unit_price) "
                   + "VALUES (?, ?, ?, ?, ?) RETURNING created_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, medication.getName());
            ps.setString(3, medication.getGenericName());
            ps.setString(4, medication.getForm());
            ps.setBigDecimal(5, medication.getUnitPrice());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    medication.setMedicationId(id.toString());
                    medication.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    medication.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save medication: " + e.getMessage(), e);
        }
        return medication;
    }

    @Override
    public Optional<Medication> findById(String medicationId) throws Exception {
        return findOneWhere("medication_id = ?", UUID.fromString(medicationId));
    }

    @Override
    public Optional<Medication> findByName(String name) throws Exception {
        return findOneWhere("name = ?", name);
    }

    private Optional<Medication> findOneWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM medications WHERE " + predicate + " AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up medication: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Medication> findAll() throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM medications WHERE deleted_at IS NULL ORDER BY name";
        List<Medication> medications = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) medications.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list medications: " + e.getMessage(), e);
        }
        return medications;
    }

    @Override
    public Medication update(Medication medication) throws Exception {
        String sql = "UPDATE medications SET name = ?, generic_name = ?, form = ?, unit_price = ? "
                   + "WHERE medication_id = ? AND deleted_at IS NULL RETURNING updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, medication.getName());
            ps.setString(2, medication.getGenericName());
            ps.setString(3, medication.getForm());
            ps.setBigDecimal(4, medication.getUnitPrice());
            ps.setObject(5, UUID.fromString(medication.getMedicationId()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ResourceNotFoundException("Medication", medication.getMedicationId());
                medication.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update medication: " + e.getMessage(), e);
        }
        return medication;
    }

    @Override
    public void softDelete(String medicationId) throws Exception {
        String sql = "UPDATE medications SET deleted_at = CURRENT_TIMESTAMP WHERE medication_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(medicationId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("Medication", medicationId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete medication: " + e.getMessage(), e);
        }
    }

    private Medication mapRow(ResultSet rs) throws SQLException {
        Medication m = new Medication();
        m.setMedicationId(rs.getObject("medication_id", UUID.class).toString());
        m.setName(rs.getString("name"));
        m.setGenericName(rs.getString("generic_name"));
        m.setForm(rs.getString("form"));
        m.setUnitPrice(rs.getBigDecimal("unit_price"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        m.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        m.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        m.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return m;
    }
}
