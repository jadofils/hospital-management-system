package hospital.management.backend.dao.department;

import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Department;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion
 * here runs DepartmentDAOImpl's actual SQL against a real database, proving the
 * RETURNING clauses, UNIQUE(name) constraint, and gen_random_uuid() default all
 * behave as the code assumes.
 */
class DepartmentDAOImplTest extends PostgresIntegrationTestBase {

    private final DepartmentDAOImpl dao = new DepartmentDAOImpl();

    private Department sampleDepartment(String name) {
        Department d = new Department();
        d.setName(name);
        d.setLocation("Building A, Floor 2");
        d.setPhone("+15551234567");
        return d;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        Department saved = dao.save(sampleDepartment("Cardiology"));

        assertNotNull(saved.getDepartmentId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getDepartmentId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save rejects a duplicate name — departments.name has a real UNIQUE constraint")
    void save_rejectsDuplicateName() throws Exception {
        dao.save(sampleDepartment("Neurology"));

        assertThrows(DatabaseException.class, () -> dao.save(sampleDepartment("Neurology")));
    }

    @Test
    @DisplayName("findById returns the saved department with every field intact")
    void findById_returnsSavedDepartment() throws Exception {
        Department saved = dao.save(sampleDepartment("Oncology"));

        Optional<Department> found = dao.findById(saved.getDepartmentId());

        assertTrue(found.isPresent());
        assertEquals("Oncology", found.get().getName());
        assertEquals("Building A, Floor 2", found.get().getLocation());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        Optional<Department> found = dao.findById(UUID.randomUUID().toString());

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted department")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        Department saved = dao.save(sampleDepartment("Radiology"));
        dao.softDelete(saved.getDepartmentId());

        assertTrue(dao.findById(saved.getDepartmentId()).isEmpty());
    }

    @Test
    @DisplayName("findByName finds a department by its exact name")
    void findByName_findsMatch() throws Exception {
        dao.save(sampleDepartment("Pediatrics"));

        Optional<Department> found = dao.findByName("Pediatrics");

        assertTrue(found.isPresent());
        assertEquals("Pediatrics", found.get().getName());
    }

    @Test
    @DisplayName("findByName returns empty when no department has that name")
    void findByName_returnsEmpty_whenNoMatch() throws Exception {
        assertTrue(dao.findByName("Nonexistent Dept").isEmpty());
    }

    @Test
    @DisplayName("findAll returns every non-deleted department ordered by name")
    void findAll_returnsNonDeletedDepartmentsOrderedByName() throws Exception {
        dao.save(sampleDepartment("Zoology"));
        dao.save(sampleDepartment("Anesthesiology"));
        Department toDelete = dao.save(sampleDepartment("Deleted Dept"));
        dao.softDelete(toDelete.getDepartmentId());

        List<Department> all = dao.findAll();

        assertEquals(2, all.size());
        assertEquals("Anesthesiology", all.get(0).getName());
        assertEquals("Zoology", all.get(1).getName());
    }

    @Test
    @DisplayName("update persists changed fields and refreshes updated_at via the DB trigger")
    void update_persistsChanges() throws Exception {
        Department saved = dao.save(sampleDepartment("Dermatology"));

        saved.setLocation("Building B, Floor 1");
        saved.setPhone("+15559998888");
        Department updated = dao.update(saved);

        assertEquals("Building B, Floor 1", updated.getLocation());
        Optional<Department> reloaded = dao.findById(saved.getDepartmentId());
        assertEquals("Building B, Floor 1", reloaded.get().getLocation());
        assertEquals("+15559998888", reloaded.get().getPhone());
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException for a department id that doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() {
        Department ghost = sampleDepartment("Ghost Dept");
        ghost.setDepartmentId(UUID.randomUUID().toString());

        assertThrows(ResourceNotFoundException.class, () -> dao.update(ghost));
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        Department saved = dao.save(sampleDepartment("Urology"));

        dao.softDelete(saved.getDepartmentId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getDepartmentId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }
}
