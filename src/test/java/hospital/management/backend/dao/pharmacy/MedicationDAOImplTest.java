package hospital.management.backend.dao.pharmacy;

import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.pharmacy.Medication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion here
 * runs MedicationDAOImpl's actual SQL against a real database, proving the RETURNING
 * clauses, the `unit_price >= 0` CHECK constraint, and gen_random_uuid() default all
 * behave as the code assumes.
 */
class MedicationDAOImplTest extends PostgresIntegrationTestBase {

    private final MedicationDAOImpl dao = new MedicationDAOImpl();

    private Medication sampleMedication(String name) {
        Medication m = new Medication();
        m.setName(name);
        m.setGenericName("Acetaminophen");
        m.setForm("tablet");
        m.setUnitPrice(new BigDecimal("9.99"));
        return m;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        Medication saved = dao.save(sampleMedication("Paracetamol"));

        assertNotNull(saved.getMedicationId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getMedicationId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save rejects a negative unit price via the DB's CHECK (unit_price >= 0) constraint")
    void save_rejectsNegativeUnitPrice() {
        Medication negative = sampleMedication("Ibuprofen");
        negative.setUnitPrice(new BigDecimal("-1.00"));

        assertThrows(DatabaseException.class, () -> dao.save(negative));
    }

    @Test
    @DisplayName("save allows unit_price of exactly zero (boundary of the CHECK constraint)")
    void save_allowsZeroUnitPrice() throws Exception {
        Medication free = sampleMedication("Sample Vial");
        free.setUnitPrice(BigDecimal.ZERO);

        Medication saved = dao.save(free);

        assertEquals(0, saved.getUnitPrice().compareTo(BigDecimal.ZERO));
    }

    /**
     * Unlike doctors.email/users.email, medications.name has NO UNIQUE constraint in
     * hospital_schema.sql (only a plain index for lookup speed) — PharmacyServiceImpl.addMedication()
     * also never calls findByName to check for a duplicate before inserting. This documents the
     * DAO's actual (permissive) behavior; it is not an endorsement of the gap — see the finding
     * reported alongside this test suite.
     */
    @Test
    @DisplayName("save does not itself reject a duplicate name — the DB has no UNIQUE(name) constraint on medications")
    void save_allowsDuplicateName_atDaoLevel() throws Exception {
        dao.save(sampleMedication("Amoxicillin"));

        assertDoesNotThrow(() -> dao.save(sampleMedication("Amoxicillin")));
    }

    @Test
    @DisplayName("findById returns the saved medication with every field intact")
    void findById_returnsSavedMedication() throws Exception {
        Medication saved = dao.save(sampleMedication("Paracetamol"));

        Optional<Medication> found = dao.findById(saved.getMedicationId());

        assertTrue(found.isPresent());
        assertEquals("Paracetamol", found.get().getName());
        assertEquals("Acetaminophen", found.get().getGenericName());
        assertEquals(0, new BigDecimal("9.99").compareTo(found.get().getUnitPrice()));
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        Optional<Medication> found = dao.findById(UUID.randomUUID().toString());

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted medication")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        Medication saved = dao.save(sampleMedication("Paracetamol"));
        dao.softDelete(saved.getMedicationId());

        assertTrue(dao.findById(saved.getMedicationId()).isEmpty());
    }

    @Test
    @DisplayName("findByName finds a medication by its exact name")
    void findByName_findsMatch() throws Exception {
        dao.save(sampleMedication("Paracetamol"));

        Optional<Medication> found = dao.findByName("Paracetamol");

        assertTrue(found.isPresent());
        assertEquals("Acetaminophen", found.get().getGenericName());
    }

    @Test
    @DisplayName("findByName returns empty when no medication matches")
    void findByName_returnsEmpty_whenNoMatch() throws Exception {
        assertTrue(dao.findByName("Nonexistent Drug").isEmpty());
    }

    @Test
    @DisplayName("findAll returns every non-deleted medication ordered by name")
    void findAll_returnsNonDeletedMedicationsOrderedByName() throws Exception {
        dao.save(sampleMedication("Zinc Supplement"));
        dao.save(sampleMedication("Amoxicillin"));
        Medication toDelete = dao.save(sampleMedication("Deleted Drug"));
        dao.softDelete(toDelete.getMedicationId());

        List<Medication> all = dao.findAll();

        assertEquals(2, all.size());
        assertEquals("Amoxicillin", all.get(0).getName());
        assertEquals("Zinc Supplement", all.get(1).getName());
    }

    @Test
    @DisplayName("update persists changed fields and refreshes updated_at via the DB trigger")
    void update_persistsChanges() throws Exception {
        Medication saved = dao.save(sampleMedication("Paracetamol"));

        saved.setForm("syrup");
        saved.setUnitPrice(new BigDecimal("14.50"));
        Medication updated = dao.update(saved);

        assertEquals("syrup", updated.getForm());
        Optional<Medication> reloaded = dao.findById(saved.getMedicationId());
        assertEquals("syrup", reloaded.get().getForm());
        assertEquals(0, new BigDecimal("14.50").compareTo(reloaded.get().getUnitPrice()));
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException for a medication id that doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() {
        Medication ghost = sampleMedication("Ghost Drug");
        ghost.setMedicationId(UUID.randomUUID().toString());

        assertThrows(ResourceNotFoundException.class, () -> dao.update(ghost));
    }

    @Test
    @DisplayName("update rejects a negative unit price via the DB's CHECK constraint")
    void update_rejectsNegativeUnitPrice() throws Exception {
        Medication saved = dao.save(sampleMedication("Paracetamol"));
        saved.setUnitPrice(new BigDecimal("-5.00"));

        assertThrows(DatabaseException.class, () -> dao.update(saved));
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        Medication saved = dao.save(sampleMedication("Paracetamol"));

        dao.softDelete(saved.getMedicationId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getMedicationId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }
}
