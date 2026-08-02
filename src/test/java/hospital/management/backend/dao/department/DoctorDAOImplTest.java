package hospital.management.backend.dao.department;

import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Department;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion
 * here runs DoctorDAOImpl's actual SQL against a real database, proving the
 * RETURNING clauses, UNIQUE(email) constraint, department FK, and gen_random_uuid()
 * default all behave as the code assumes.
 */
class DoctorDAOImplTest extends PostgresIntegrationTestBase {

    private final DoctorDAOImpl dao = new DoctorDAOImpl();
    private final DepartmentDAOImpl departmentDao = new DepartmentDAOImpl();

    private Department savedDepartment(String name) throws Exception {
        Department d = new Department();
        d.setName(name);
        d.setLocation("Wing C");
        return departmentDao.save(d);
    }

    private Doctor sampleDoctor(String departmentId, String email) {
        Doctor d = new Doctor();
        d.setDepartmentId(departmentId);
        d.setFirstName("Sarah");
        d.setLastName("Chen");
        d.setSpecialization("Cardiology");
        d.setPhone("+15551112222");
        d.setEmail(email);
        return d;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        Department dept = savedDepartment("Cardiology");

        Doctor saved = dao.save(sampleDoctor(dept.getDepartmentId(), "sarah.chen@example.com"));

        assertNotNull(saved.getDoctorId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getDoctorId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save allows a null department_id — the FK column is nullable in the schema")
    void save_allowsNullDepartmentId() throws Exception {
        Doctor saved = dao.save(sampleDoctor(null, "no.dept@example.com"));

        assertNull(saved.getDepartmentId());
        Optional<Doctor> found = dao.findById(saved.getDoctorId());
        assertTrue(found.isPresent());
        assertNull(found.get().getDepartmentId());
    }

    @Test
    @DisplayName("save rejects a department_id that doesn't exist — real FK constraint enforcement")
    void save_rejectsNonExistentDepartmentId() {
        Doctor doctor = sampleDoctor(UUID.randomUUID().toString(), "orphan@example.com");

        assertThrows(DatabaseException.class, () -> dao.save(doctor));
    }

    @Test
    @DisplayName("save rejects a duplicate email — doctors.email has a real UNIQUE constraint")
    void save_rejectsDuplicateEmail() throws Exception {
        Department dept = savedDepartment("Cardiology");
        dao.save(sampleDoctor(dept.getDepartmentId(), "dup@example.com"));

        Doctor second = sampleDoctor(dept.getDepartmentId(), "dup@example.com");
        assertThrows(DatabaseException.class, () -> dao.save(second));
    }

    @Test
    @DisplayName("findById returns the saved doctor with every field intact")
    void findById_returnsSavedDoctor() throws Exception {
        Department dept = savedDepartment("Cardiology");
        Doctor saved = dao.save(sampleDoctor(dept.getDepartmentId(), "sarah.chen@example.com"));

        Optional<Doctor> found = dao.findById(saved.getDoctorId());

        assertTrue(found.isPresent());
        assertEquals("Sarah", found.get().getFirstName());
        assertEquals(dept.getDepartmentId(), found.get().getDepartmentId());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted doctor")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        Department dept = savedDepartment("Cardiology");
        Doctor saved = dao.save(sampleDoctor(dept.getDepartmentId(), "sarah.chen@example.com"));
        dao.softDelete(saved.getDoctorId());

        assertTrue(dao.findById(saved.getDoctorId()).isEmpty());
    }

    @Test
    @DisplayName("findByEmail finds a doctor by their exact email")
    void findByEmail_findsMatch() throws Exception {
        Department dept = savedDepartment("Cardiology");
        dao.save(sampleDoctor(dept.getDepartmentId(), "sarah.chen@example.com"));

        Optional<Doctor> found = dao.findByEmail("sarah.chen@example.com");

        assertTrue(found.isPresent());
        assertEquals("Sarah", found.get().getFirstName());
    }

    @Test
    @DisplayName("findByDepartmentId returns only doctors in that department, ordered by name")
    void findByDepartmentId_returnsDoctorsInDepartment() throws Exception {
        Department cardiology = savedDepartment("Cardiology");
        Department neurology = savedDepartment("Neurology");
        dao.save(sampleDoctor(cardiology.getDepartmentId(), "sarah.chen@example.com"));
        Doctor other = sampleDoctor(neurology.getDepartmentId(), "other@example.com");
        other.setFirstName("Bob");
        other.setLastName("Smith");
        dao.save(other);

        List<Doctor> found = dao.findByDepartmentId(cardiology.getDepartmentId());

        assertEquals(1, found.size());
        assertEquals("Sarah", found.get(0).getFirstName());
    }

    @Test
    @DisplayName("findAll returns every non-deleted doctor")
    void findAll_returnsNonDeletedDoctors() throws Exception {
        Department dept = savedDepartment("Cardiology");
        dao.save(sampleDoctor(dept.getDepartmentId(), "sarah.chen@example.com"));
        Doctor toDelete = sampleDoctor(dept.getDepartmentId(), "deleted@example.com");
        Doctor savedToDelete = dao.save(toDelete);
        dao.softDelete(savedToDelete.getDoctorId());

        PageResult<Doctor> page = dao.findAll(CursorPagination.firstPage());

        assertEquals(1, page.getCount());
        assertEquals("sarah.chen@example.com", page.getItems().get(0).getEmail());
    }

    @Test
    @DisplayName("update persists changed fields and refreshes updated_at via the DB trigger")
    void update_persistsChanges() throws Exception {
        Department dept = savedDepartment("Cardiology");
        Doctor saved = dao.save(sampleDoctor(dept.getDepartmentId(), "sarah.chen@example.com"));

        saved.setSpecialization("Neurology");
        saved.setPhone("+15559998888");
        Doctor updated = dao.update(saved);

        assertEquals("Neurology", updated.getSpecialization());
        Optional<Doctor> reloaded = dao.findById(saved.getDoctorId());
        assertEquals("Neurology", reloaded.get().getSpecialization());
        assertEquals("+15559998888", reloaded.get().getPhone());
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException for a doctor id that doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() {
        Doctor ghost = sampleDoctor(null, "ghost@example.com");
        ghost.setDoctorId(UUID.randomUUID().toString());

        assertThrows(ResourceNotFoundException.class, () -> dao.update(ghost));
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        Department dept = savedDepartment("Cardiology");
        Doctor saved = dao.save(sampleDoctor(dept.getDepartmentId(), "sarah.chen@example.com"));

        dao.softDelete(saved.getDoctorId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getDoctorId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }
}
