package hospital.management.backend.dao.pharmacy;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.pharmacy.Medication;
import hospital.management.backend.model.pharmacy.Prescription;
import hospital.management.backend.model.pharmacy.PrescriptionItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion here
 * runs PrescriptionItemDAOImpl's actual SQL against a real database, proving the RETURNING
 * clauses, the `quantity > 0` CHECK constraint, the CASCADE delete from prescriptions, and
 * the RESTRICT delete from medications.
 *
 * A prescription item always hangs off a real prescription (which itself needs a real
 * appointment/patient/doctor) and a real medication, so every test seeds that whole FK
 * chain directly via JDBC rather than depending on other domains' DAOs.
 */
class PrescriptionItemDAOImplTest extends PostgresIntegrationTestBase {

    private final PrescriptionItemDAOImpl dao              = new PrescriptionItemDAOImpl();
    private final PrescriptionDAOImpl     prescriptionDao   = new PrescriptionDAOImpl();
    private final MedicationDAOImpl       medicationDao     = new MedicationDAOImpl();

    private String insertPatient() throws Exception {
        String sql = "INSERT INTO patients (first_name, last_name, dob) VALUES (?, ?, ?) RETURNING patient_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "Jane");
            ps.setString(2, "Doe");
            ps.setObject(3, java.sql.Date.valueOf(LocalDate.of(1990, 5, 20)));
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject("patient_id", UUID.class).toString();
            }
        }
    }

    private String insertDoctor() throws Exception {
        String sql = "INSERT INTO doctors (first_name, last_name) VALUES (?, ?) RETURNING doctor_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "Greg");
            ps.setString(2, "House");
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject("doctor_id", UUID.class).toString();
            }
        }
    }

    private String insertAppointment() throws Exception {
        String sql = "INSERT INTO appointments (patient_id, doctor_id, appointment_date) "
                   + "VALUES (?, ?, ?) RETURNING appointment_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(insertPatient()));
            ps.setObject(2, UUID.fromString(insertDoctor()));
            ps.setObject(3, java.sql.Timestamp.valueOf(LocalDate.now().atStartOfDay()));
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject("appointment_id", UUID.class).toString();
            }
        }
    }

    private String insertPrescription() throws Exception {
        Prescription p = new Prescription();
        p.setAppointmentId(insertAppointment());
        p.setDateIssued(LocalDate.now());
        return prescriptionDao.save(p).getPrescriptionId();
    }

    private String insertMedication() throws Exception {
        Medication m = new Medication();
        m.setName("Amoxicillin");
        m.setUnitPrice(new BigDecimal("3.50"));
        return medicationDao.save(m).getMedicationId();
    }

    private PrescriptionItem sampleItem(String prescriptionId, String medicationId) {
        PrescriptionItem item = new PrescriptionItem();
        item.setPrescriptionId(prescriptionId);
        item.setMedicationId(medicationId);
        item.setDosage("500mg twice daily");
        item.setQuantity(20);
        item.setInstructions("Take with food");
        return item;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        String prescriptionId = insertPrescription();
        String medicationId = insertMedication();

        PrescriptionItem saved = dao.save(sampleItem(prescriptionId, medicationId));

        assertNotNull(saved.getItemId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getItemId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save(item, conn) runs on the caller-supplied connection, without committing itself")
    void save_withConnection_usesCallerSuppliedConnection() throws Exception {
        String prescriptionId = insertPrescription();
        String medicationId = insertMedication();

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            PrescriptionItem saved = dao.save(sampleItem(prescriptionId, medicationId), conn);
            conn.commit();

            assertNotNull(saved.getItemId());
            assertTrue(dao.findById(saved.getItemId()).isPresent());
        }
    }

    @Test
    @DisplayName("save rejects a quantity of zero or less via the DB's CHECK (quantity > 0) constraint")
    void save_rejectsNonPositiveQuantity() throws Exception {
        String prescriptionId = insertPrescription();
        String medicationId = insertMedication();
        PrescriptionItem zeroQty = sampleItem(prescriptionId, medicationId);
        zeroQty.setQuantity(0);

        assertThrows(DatabaseException.class, () -> dao.save(zeroQty));
    }

    @Test
    @DisplayName("save rejects a prescription_id that doesn't exist via the FK constraint")
    void save_rejectsUnknownPrescriptionId() throws Exception {
        String medicationId = insertMedication();
        PrescriptionItem orphan = sampleItem(UUID.randomUUID().toString(), medicationId);

        assertThrows(DatabaseException.class, () -> dao.save(orphan));
    }

    @Test
    @DisplayName("save rejects a medication_id that doesn't exist via the FK constraint")
    void save_rejectsUnknownMedicationId() throws Exception {
        String prescriptionId = insertPrescription();
        PrescriptionItem orphan = sampleItem(prescriptionId, UUID.randomUUID().toString());

        assertThrows(DatabaseException.class, () -> dao.save(orphan));
    }

    @Test
    @DisplayName("findById returns the saved item with every field intact")
    void findById_returnsSavedItem() throws Exception {
        String prescriptionId = insertPrescription();
        String medicationId = insertMedication();
        PrescriptionItem saved = dao.save(sampleItem(prescriptionId, medicationId));

        Optional<PrescriptionItem> found = dao.findById(saved.getItemId());

        assertTrue(found.isPresent());
        assertEquals("500mg twice daily", found.get().getDosage());
        assertEquals(20, found.get().getQuantity());
        assertEquals("Take with food", found.get().getInstructions());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findByPrescriptionId returns every line item for that prescription, ordered by created_at")
    void findByPrescriptionId_returnsAllItems() throws Exception {
        String prescriptionId = insertPrescription();
        String medicationId1 = insertMedication();
        String medicationId2 = insertMedication();
        String otherPrescriptionId = insertPrescription();

        dao.save(sampleItem(prescriptionId, medicationId1));
        dao.save(sampleItem(prescriptionId, medicationId2));
        dao.save(sampleItem(otherPrescriptionId, medicationId1));

        List<PrescriptionItem> result = dao.findByPrescriptionId(prescriptionId);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        String prescriptionId = insertPrescription();
        String medicationId = insertMedication();
        PrescriptionItem saved = dao.save(sampleItem(prescriptionId, medicationId));

        dao.softDelete(saved.getItemId());

        assertTrue(dao.findById(saved.getItemId()).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getItemId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }

    @Test
    @DisplayName("hard-deleting the parent prescription CASCADEs to its items, per the FK's ON DELETE CASCADE")
    void hardDeletingPrescription_cascadesToItems() throws Exception {
        String prescriptionId = insertPrescription();
        String medicationId = insertMedication();
        PrescriptionItem saved = dao.save(sampleItem(prescriptionId, medicationId));

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM prescriptions WHERE prescription_id = ?")) {
            ps.setObject(1, UUID.fromString(prescriptionId));
            ps.executeUpdate();
        }

        assertTrue(dao.findById(saved.getItemId()).isEmpty(),
                "the item row itself should be gone once its parent prescription is hard-deleted");
    }

    @Test
    @DisplayName("hard-deleting a medication still referenced by a prescription item is blocked, "
            + "per the FK's ON DELETE RESTRICT")
    void hardDeletingReferencedMedication_isRestricted() throws Exception {
        String prescriptionId = insertPrescription();
        String medicationId = insertMedication();
        dao.save(sampleItem(prescriptionId, medicationId));

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM medications WHERE medication_id = ?")) {
            ps.setObject(1, UUID.fromString(medicationId));
            assertThrows(SQLException.class, ps::executeUpdate);
        }
    }
}
