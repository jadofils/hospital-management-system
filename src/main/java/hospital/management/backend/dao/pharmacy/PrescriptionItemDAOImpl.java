package hospital.management.backend.dao.pharmacy;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.pharmacy.interfaces.PrescriptionItemDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.pharmacy.PrescriptionItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `prescription_items` table (see hospital_schema.sql). */
public class PrescriptionItemDAOImpl implements PrescriptionItemDAO {

    private static final String SELECT_COLUMNS =
        "item_id, prescription_id, medication_id, dosage, quantity, instructions, created_at, updated_at, deleted_at";

    @Override
    public PrescriptionItem save(PrescriptionItem item) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return save(item, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save prescription item: " + e.getMessage(), e);
        }
    }

    @Override
    public PrescriptionItem save(PrescriptionItem item, Connection conn) throws Exception {
        UUID id = item.getItemId() != null ? UUID.fromString(item.getItemId()) : UUID.randomUUID();
        String sql = "INSERT INTO prescription_items (item_id, prescription_id, medication_id, dosage, quantity, instructions) "
                   + "VALUES (?, ?, ?, ?, ?, ?) RETURNING created_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(item.getPrescriptionId()));
            ps.setObject(3, UUID.fromString(item.getMedicationId()));
            ps.setString(4, item.getDosage());
            ps.setInt(5, item.getQuantity());
            ps.setString(6, item.getInstructions());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    item.setItemId(id.toString());
                    item.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    item.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return item;
    }

    @Override
    public Optional<PrescriptionItem> findById(String itemId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM prescription_items WHERE item_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(itemId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up prescription item: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PrescriptionItem> findByPrescriptionId(String prescriptionId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM prescription_items "
                   + "WHERE prescription_id = ? AND deleted_at IS NULL ORDER BY created_at";
        List<PrescriptionItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(prescriptionId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list prescription items: " + e.getMessage(), e);
        }
        return items;
    }

    @Override
    public void softDelete(String itemId) throws Exception {
        String sql = "UPDATE prescription_items SET deleted_at = CURRENT_TIMESTAMP WHERE item_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(itemId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("PrescriptionItem", itemId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete prescription item: " + e.getMessage(), e);
        }
    }

    private PrescriptionItem mapRow(ResultSet rs) throws SQLException {
        PrescriptionItem item = new PrescriptionItem();
        item.setItemId(rs.getObject("item_id", UUID.class).toString());
        item.setPrescriptionId(rs.getObject("prescription_id", UUID.class).toString());
        item.setMedicationId(rs.getObject("medication_id", UUID.class).toString());
        item.setDosage(rs.getString("dosage"));
        item.setQuantity(rs.getInt("quantity"));
        item.setInstructions(rs.getString("instructions"));
        item.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        item.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        item.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return item;
    }
}
