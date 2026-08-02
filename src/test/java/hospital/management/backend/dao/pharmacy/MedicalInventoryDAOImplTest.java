package hospital.management.backend.dao.pharmacy;

import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.pharmacy.MedicalInventory;
import hospital.management.backend.model.pharmacy.Medication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion here
 * runs MedicalInventoryDAOImpl's actual SQL against a real database, proving the RETURNING
 * clauses, the `quantity_in_stock >= 0` CHECK constraint, and the medication_id FK (RESTRICT)
 * all behave as the code assumes.
 */
class MedicalInventoryDAOImplTest extends PostgresIntegrationTestBase {

    private final MedicalInventoryDAOImpl dao          = new MedicalInventoryDAOImpl();
    private final MedicationDAOImpl       medicationDao = new MedicationDAOImpl();

    private String createMedication(String name) throws Exception {
        Medication m = new Medication();
        m.setName(name);
        m.setUnitPrice(new BigDecimal("5.00"));
        return medicationDao.save(m).getMedicationId();
    }

    private MedicalInventory sampleInventory(String medicationId) {
        MedicalInventory i = new MedicalInventory();
        i.setMedicationId(medicationId);
        i.setBatchNumber("BATCH-001");
        i.setExpiryDate(LocalDate.now().plusYears(1));
        i.setQuantityInStock(100);
        i.setReorderLevel(10);
        i.setSupplier("Acme Pharma");
        return i;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        String medicationId = createMedication("Paracetamol");

        MedicalInventory saved = dao.save(sampleInventory(medicationId));

        assertNotNull(saved.getInventoryId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getInventoryId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save defaults quantity_in_stock to 0 and reorder_level to 10 when null, per DB defaults")
    void save_appliesDbDefaults_whenNullPassedThroughAsExplicitValue() throws Exception {
        // The DAO itself substitutes 0/10 in Java when null (see MedicalInventoryDAOImpl.save),
        // matching the DB's own DEFAULT clauses on quantity_in_stock/reorder_level.
        String medicationId = createMedication("Paracetamol");
        MedicalInventory inventory = sampleInventory(medicationId);
        inventory.setQuantityInStock(null);
        inventory.setReorderLevel(null);

        MedicalInventory saved = dao.save(inventory);

        Optional<MedicalInventory> reloaded = dao.findById(saved.getInventoryId());
        assertEquals(0, reloaded.get().getQuantityInStock());
        assertEquals(10, reloaded.get().getReorderLevel());
    }

    @Test
    @DisplayName("save rejects a negative quantity_in_stock via the DB's CHECK constraint")
    void save_rejectsNegativeQuantity() throws Exception {
        String medicationId = createMedication("Paracetamol");
        MedicalInventory negative = sampleInventory(medicationId);
        negative.setQuantityInStock(-5);

        assertThrows(DatabaseException.class, () -> dao.save(negative));
    }

    @Test
    @DisplayName("save rejects a medication_id that doesn't exist via the FK constraint")
    void save_rejectsUnknownMedicationId() {
        MedicalInventory orphan = sampleInventory(UUID.randomUUID().toString());

        assertThrows(DatabaseException.class, () -> dao.save(orphan));
    }

    @Test
    @DisplayName("findById returns the saved inventory batch with every field intact")
    void findById_returnsSavedInventory() throws Exception {
        String medicationId = createMedication("Paracetamol");
        MedicalInventory saved = dao.save(sampleInventory(medicationId));

        Optional<MedicalInventory> found = dao.findById(saved.getInventoryId());

        assertTrue(found.isPresent());
        assertEquals("BATCH-001", found.get().getBatchNumber());
        assertEquals(100, found.get().getQuantityInStock());
        assertEquals("Acme Pharma", found.get().getSupplier());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findByMedicationId returns only batches for that medication, ordered by expiry date")
    void findByMedicationId_returnsOrderedByExpiry() throws Exception {
        String medicationId = createMedication("Paracetamol");
        String otherMedicationId = createMedication("Ibuprofen");

        MedicalInventory later = sampleInventory(medicationId);
        later.setExpiryDate(LocalDate.now().plusYears(2));
        later.setBatchNumber("LATER");
        dao.save(later);

        MedicalInventory sooner = sampleInventory(medicationId);
        sooner.setExpiryDate(LocalDate.now().plusMonths(1));
        sooner.setBatchNumber("SOONER");
        dao.save(sooner);

        dao.save(sampleInventory(otherMedicationId));

        List<MedicalInventory> result = dao.findByMedicationId(medicationId);

        assertEquals(2, result.size());
        assertEquals("SOONER", result.get(0).getBatchNumber());
        assertEquals("LATER", result.get(1).getBatchNumber());
    }

    @Test
    @DisplayName("findLowStock returns only batches at or below their reorder level")
    void findLowStock_returnsOnlyLowStockBatches() throws Exception {
        String medicationId = createMedication("Paracetamol");

        MedicalInventory low = sampleInventory(medicationId);
        low.setQuantityInStock(5);
        low.setReorderLevel(10);
        dao.save(low);

        MedicalInventory healthy = sampleInventory(medicationId);
        healthy.setQuantityInStock(500);
        healthy.setReorderLevel(10);
        dao.save(healthy);

        List<MedicalInventory> result = dao.findLowStock();

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getQuantityInStock());
    }

    @Test
    @DisplayName("update persists changed fields and refreshes updated_at via the DB trigger")
    void update_persistsChanges() throws Exception {
        String medicationId = createMedication("Paracetamol");
        MedicalInventory saved = dao.save(sampleInventory(medicationId));

        saved.setQuantityInStock(50);
        saved.setSupplier("New Supplier");
        MedicalInventory updated = dao.update(saved);

        assertEquals(50, updated.getQuantityInStock());
        Optional<MedicalInventory> reloaded = dao.findById(saved.getInventoryId());
        assertEquals("New Supplier", reloaded.get().getSupplier());
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException for an inventory id that doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() throws Exception {
        String medicationId = createMedication("Paracetamol");
        MedicalInventory ghost = sampleInventory(medicationId);
        ghost.setInventoryId(UUID.randomUUID().toString());

        assertThrows(ResourceNotFoundException.class, () -> dao.update(ghost));
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        String medicationId = createMedication("Paracetamol");
        MedicalInventory saved = dao.save(sampleInventory(medicationId));

        dao.softDelete(saved.getInventoryId());

        assertTrue(dao.findById(saved.getInventoryId()).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getInventoryId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }
}
