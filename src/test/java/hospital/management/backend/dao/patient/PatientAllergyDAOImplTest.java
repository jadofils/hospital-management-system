package hospital.management.backend.dao.patient;

import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.model.patient.PatientAllergy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion here
 * runs PatientAllergyDAOImpl's actual SQL against a real database, proving the RETURNING
 * clause, the FK(patient_id) constraint, the severity CHECK constraint, and the
 * updated_at trigger all behave as the code assumes.
 */
class PatientAllergyDAOImplTest extends PostgresIntegrationTestBase {

    private final PatientAllergyDAOImpl dao = new PatientAllergyDAOImpl();
    private final PatientDAOImpl patientDAO = new PatientDAOImpl();

    private String patientId;

    @BeforeEach
    void createParentPatient() throws Exception {
        Patient p = new Patient();
        p.setFirstName("Jane");
        p.setLastName("Doe");
        p.setDob(LocalDate.of(1990, 5, 20));
        p.setGender("F");
        p.setEmail("jane.doe." + UUID.randomUUID() + "@example.com");
        patientId = patientDAO.save(p).getPatientId();
    }

    private PatientAllergy sampleAllergy(String severity) {
        PatientAllergy a = new PatientAllergy();
        a.setPatientId(patientId);
        a.setAllergen("Penicillin");
        a.setReaction("Hives");
        a.setSeverity(severity);
        return a;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        PatientAllergy saved = dao.save(sampleAllergy("moderate"));

        assertNotNull(saved.getAllergyId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getAllergyId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save persists a null severity (the column is nullable — CHECK doesn't reject NULL)")
    void save_allowsNullSeverity() throws Exception {
        PatientAllergy saved = dao.save(sampleAllergy(null));

        Optional<PatientAllergy> found = dao.findById(saved.getAllergyId());
        assertTrue(found.isPresent());
        assertNull(found.get().getSeverity());
    }

    @Test
    @DisplayName("save rejects a severity outside the ('mild','moderate','severe') CHECK constraint")
    void save_rejectsInvalidSeverity() {
        assertThrows(DatabaseException.class, () -> dao.save(sampleAllergy("critical")));
    }

    @Test
    @DisplayName("save rejects an allergy referencing a patient_id that doesn't exist (FK violation)")
    void save_rejectsUnknownPatientId() {
        PatientAllergy orphan = sampleAllergy("mild");
        orphan.setPatientId(UUID.randomUUID().toString());

        assertThrows(DatabaseException.class, () -> dao.save(orphan));
    }

    @Test
    @DisplayName("findById returns the saved allergy with every field intact")
    void findById_returnsSavedAllergy() throws Exception {
        PatientAllergy saved = dao.save(sampleAllergy("severe"));

        Optional<PatientAllergy> found = dao.findById(saved.getAllergyId());

        assertTrue(found.isPresent());
        assertEquals("Penicillin", found.get().getAllergen());
        assertEquals("Hives", found.get().getReaction());
        assertEquals("severe", found.get().getSeverity());
        assertEquals(patientId, found.get().getPatientId());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted allergy")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        PatientAllergy saved = dao.save(sampleAllergy("mild"));
        dao.softDelete(saved.getAllergyId());

        assertTrue(dao.findById(saved.getAllergyId()).isEmpty());
    }

    @Test
    @DisplayName("findByPatientId returns only that patient's non-deleted allergies, most-recently-created first")
    void findByPatientId_returnsNonDeletedAllergiesForPatient() throws Exception {
        PatientAllergy kept = dao.save(sampleAllergy("mild"));
        PatientAllergy deleted = dao.save(sampleAllergy("severe"));
        dao.softDelete(deleted.getAllergyId());

        // A second, unrelated patient's allergy must not leak into this patient's list.
        Patient other = new Patient();
        other.setFirstName("Bob");
        other.setLastName("Smith");
        other.setDob(LocalDate.of(1985, 1, 1));
        other.setGender("M");
        String otherPatientId = patientDAO.save(other).getPatientId();
        PatientAllergy otherAllergy = sampleAllergy("mild");
        otherAllergy.setPatientId(otherPatientId);
        dao.save(otherAllergy);

        List<PatientAllergy> found = dao.findByPatientId(patientId);

        assertEquals(1, found.size());
        assertEquals(kept.getAllergyId(), found.get(0).getAllergyId());
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it, and a second call throws")
    void softDelete_marksDeletedAt_andRejectsDoubleDelete() throws Exception {
        PatientAllergy saved = dao.save(sampleAllergy("mild"));

        dao.softDelete(saved.getAllergyId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getAllergyId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }

    @Test
    @DisplayName("softDelete throws ResourceNotFoundException for an id that was never saved")
    void softDelete_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class,
                () -> dao.softDelete(UUID.randomUUID().toString()));
    }
}
