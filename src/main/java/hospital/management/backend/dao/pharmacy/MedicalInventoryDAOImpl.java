package hospital.management.backend.dao.pharmacy;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.pharmacy.interfaces.MedicalInventoryDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.pharmacy.MedicalInventory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `medical_inventory` table (see hospital_schema.sql). */
public class MedicalInventoryDAOImpl implements MedicalInventoryDAO {

    private static final String SELECT_COLUMNS =
        "inventory_id, medication_id, batch_number, expiry_date, quantity_in_stock, "
      + "reorder_level, supplier, created_at, updated_at, deleted_at";

    @Override
    public MedicalInventory save(MedicalInventory inventory) throws Exception {
        UUID id = inventory.getInventoryId() != null
                ? UUID.fromString(inventory.getInventoryId()) : UUID.randomUUID();
        String sql = "INSERT INTO medical_inventory "
                   + "(inventory_id, medication_id, batch_number, expiry_date, quantity_in_stock, reorder_level, supplier) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING created_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(inventory.getMedicationId()));
            ps.setString(3, inventory.getBatchNumber());
            ps.setDate(4, Date.valueOf(inventory.getExpiryDate()));
            ps.setInt(5, inventory.getQuantityInStock() != null ? inventory.getQuantityInStock() : 0);
            ps.setInt(6, inventory.getReorderLevel() != null ? inventory.getReorderLevel() : 10);
            ps.setString(7, inventory.getSupplier());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    inventory.setInventoryId(id.toString());
                    inventory.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    inventory.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save inventory batch: " + e.getMessage(), e);
        }
        return inventory;
    }

    @Override
    public Optional<MedicalInventory> findById(String inventoryId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM medical_inventory WHERE inventory_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(inventoryId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up inventory batch: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MedicalInventory> findByMedicationId(String medicationId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM medical_inventory "
                   + "WHERE medication_id = ? AND deleted_at IS NULL ORDER BY expiry_date";
        List<MedicalInventory> results = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(medicationId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list inventory for medication: " + e.getMessage(), e);
        }
        return results;
    }

    @Override
    public List<MedicalInventory> findLowStock() throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM medical_inventory "
                   + "WHERE quantity_in_stock <= reorder_level AND deleted_at IS NULL ORDER BY quantity_in_stock";
        List<MedicalInventory> results = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) results.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list low-stock inventory: " + e.getMessage(), e);
        }
        return results;
    }

    @Override
    public MedicalInventory update(MedicalInventory inventory) throws Exception {
        String sql = "UPDATE medical_inventory SET batch_number = ?, expiry_date = ?, quantity_in_stock = ?, "
                   + "reorder_level = ?, supplier = ? WHERE inventory_id = ? AND deleted_at IS NULL RETURNING updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inventory.getBatchNumber());
            ps.setDate(2, Date.valueOf(inventory.getExpiryDate()));
            ps.setInt(3, inventory.getQuantityInStock());
            ps.setInt(4, inventory.getReorderLevel());
            ps.setString(5, inventory.getSupplier());
            ps.setObject(6, UUID.fromString(inventory.getInventoryId()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ResourceNotFoundException("MedicalInventory", inventory.getInventoryId());
                inventory.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update inventory batch: " + e.getMessage(), e);
        }
        return inventory;
    }

    @Override
    public void softDelete(String inventoryId) throws Exception {
        String sql = "UPDATE medical_inventory SET deleted_at = CURRENT_TIMESTAMP WHERE inventory_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(inventoryId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("MedicalInventory", inventoryId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete inventory batch: " + e.getMessage(), e);
        }
    }

    private MedicalInventory mapRow(ResultSet rs) throws SQLException {
        MedicalInventory i = new MedicalInventory();
        i.setInventoryId(rs.getObject("inventory_id", UUID.class).toString());
        i.setMedicationId(rs.getObject("medication_id", UUID.class).toString());
        i.setBatchNumber(rs.getString("batch_number"));
        Date expiryDate = rs.getDate("expiry_date");
        i.setExpiryDate(expiryDate != null ? expiryDate.toLocalDate() : null);
        i.setQuantityInStock(rs.getInt("quantity_in_stock"));
        i.setReorderLevel(rs.getInt("reorder_level"));
        i.setSupplier(rs.getString("supplier"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        i.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        i.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        i.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return i;
    }
}
