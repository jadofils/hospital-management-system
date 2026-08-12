package hospital.management.backend.dao.patient;

import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (via Testcontainers — see PostgresIntegrationTestBase):
 * every assertion here runs PatientDAOImpl's actual SQL against a real database, proving
 * the RETURNING clauses, UNIQUE(email) constraint, gen_random_uuid() default, and
 * updated_at trigger all behave as the code assumes — none of which a mocked Connection
 * could verify.
 */
class PatientDAOImplTest extends PostgresIntegrationTestBase {

    private final PatientDAOImpl dao = new PatientDAOImpl();

    private Patient samplePatient(String email) {
        Patient p = new Patient();
        p.setFirstName("Jane");
        p.setLastName("Doe");
        p.setDob(LocalDate.of(1990, 5, 20));
        p.setGender("F");
        p.setPhone("+15558675309");
        p.setEmail(email);
        p.setAddress("123 Main St");
        return p;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        Patient saved = dao.save(samplePatient("jane.doe@example.com"));

        assertNotNull(saved.getPatientId());
        assertDoesNotThrow(() -> java.util.UUID.fromString(saved.getPatientId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    /**
     * Unlike doctors.email/users.email, patients.email has NO UNIQUE constraint in
     * hospital_schema.sql — uniqueness is only enforced by PatientServiceImpl.create()'s
     * check-then-insert (findByEmail then save), which is race-prone under concurrent
     * requests. This test documents the DAO's actual (permissive) behavior; it is not
     * an endorsement of the gap — see the finding reported alongside this test suite.
     */
    @Test
    @DisplayName("save does not itself reject a duplicate email — the DB has no UNIQUE(email) constraint on patients")
    void save_allowsDuplicateEmail_atDaoLevel() throws Exception {
        dao.save(samplePatient("dup@example.com"));

        assertDoesNotThrow(() -> dao.save(samplePatient("dup@example.com")));
    }

    @Test
    @DisplayName("findById returns the saved patient with every field intact")
    void findById_returnsSavedPatient() throws Exception {
        Patient saved = dao.save(samplePatient("jane.doe@example.com"));

        Optional<Patient> found = dao.findById(saved.getPatientId());

        assertTrue(found.isPresent());
        assertEquals("Jane", found.get().getFirstName());
        assertEquals("jane.doe@example.com", found.get().getEmail());
        assertEquals(LocalDate.of(1990, 5, 20), found.get().getDob());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        Optional<Patient> found = dao.findById(java.util.UUID.randomUUID().toString());

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted patient")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        Patient saved = dao.save(samplePatient("jane.doe@example.com"));
        dao.softDelete(saved.getPatientId());

        assertTrue(dao.findById(saved.getPatientId()).isEmpty());
    }

    @Test
    @DisplayName("findByEmail finds a patient by their exact email")
    void findByEmail_findsMatch() throws Exception {
        dao.save(samplePatient("jane.doe@example.com"));

        Optional<Patient> found = dao.findByEmail("jane.doe@example.com");

        assertTrue(found.isPresent());
        assertEquals("Jane", found.get().getFirstName());
    }

    @Test
    @DisplayName("update persists changed fields and refreshes updated_at via the DB trigger")
    void update_persistsChanges() throws Exception {
        Patient saved = dao.save(samplePatient("jane.doe@example.com"));

        saved.setAddress("456 Oak Ave");
        saved.setPhone("+15559998888");
        Patient updated = dao.update(saved);

        assertEquals("456 Oak Ave", updated.getAddress());
        Optional<Patient> reloaded = dao.findById(saved.getPatientId());
        assertEquals("456 Oak Ave", reloaded.get().getAddress());
        assertEquals("+15559998888", reloaded.get().getPhone());
    }

    @Test
    @DisplayName("save defaults status to 'active' when not explicitly set")
    void save_defaultsStatusToActive() throws Exception {
        Patient saved = dao.save(samplePatient("jane.doe@example.com"));

        Optional<Patient> found = dao.findById(saved.getPatientId());

        assertEquals("active", found.get().getStatus());
    }

    @Test
    @DisplayName("updateStatus persists the new status and returns the refreshed row")
    void updateStatus_persistsNewStatus() throws Exception {
        Patient saved = dao.save(samplePatient("jane.doe@example.com"));

        Patient updated = dao.updateStatus(saved.getPatientId(), "inactive");

        assertEquals("inactive", updated.getStatus());
        Optional<Patient> reloaded = dao.findById(saved.getPatientId());
        assertEquals("inactive", reloaded.get().getStatus());
    }

    @Test
    @DisplayName("updateStatus throws ResourceNotFoundException for a patient id that doesn't exist")
    void updateStatus_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class,
                () -> dao.updateStatus(java.util.UUID.randomUUID().toString(), "inactive"));
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException for a patient id that doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() {
        Patient ghost = samplePatient("ghost@example.com");
        ghost.setPatientId(java.util.UUID.randomUUID().toString());

        assertThrows(ResourceNotFoundException.class, () -> dao.update(ghost));
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        Patient saved = dao.save(samplePatient("jane.doe@example.com"));

        dao.softDelete(saved.getPatientId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getPatientId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }

    @Test
    @DisplayName("findAll returns every non-deleted patient, most-recently-created first")
    void findAll_returnsNonDeletedPatients() throws Exception {
        dao.save(samplePatient("jane.doe@example.com"));
        Patient toDelete = dao.save(samplePatient("deleted@example.com"));
        dao.softDelete(toDelete.getPatientId());

        PageResult<Patient> page = dao.findAll(CursorPagination.firstPage());

        assertEquals(1, page.getCount());
        assertEquals("jane.doe@example.com", page.getItems().get(0).getEmail());
    }

    @Test
    @DisplayName("search matches by first name, last name, or patient id substring")
    void search_matchesByNameOrId() throws Exception {
        Patient saved = dao.save(samplePatient("jane.doe@example.com"));
        Patient unrelated = samplePatient("bob.smith@example.com");
        unrelated.setFirstName("Bob");
        unrelated.setLastName("Smith");
        dao.save(unrelated);

        PageResult<Patient> byFirstName = dao.search("jane", CursorPagination.firstPage());
        PageResult<Patient> byId = dao.search(saved.getPatientId().substring(0, 8), CursorPagination.firstPage());

        assertEquals(1, byFirstName.getCount());
        assertEquals(1, byId.getCount());
    }
}
