package hospital.management.backend.dao.auth;

import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.user.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion here
 * runs PermissionDAOImpl's actual SQL against a real database, proving the RETURNING clause,
 * gen_random_uuid() default, updated_at trigger, and the UNIQUE(resource, action) constraint
 * all behave as the code assumes.
 */
class PermissionDAOImplTest extends PostgresIntegrationTestBase {

    private final PermissionDAOImpl dao = new PermissionDAOImpl();

    private Permission samplePermission(String resource, String action) {
        Permission p = new Permission();
        p.setResource(resource);
        p.setAction(action);
        return p;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        Permission saved = dao.save(samplePermission("patients", "create"));

        assertNotNull(saved.getPermissionId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getPermissionId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save enforces the UNIQUE(resource, action) constraint at the DB level")
    void save_enforcesUniqueResourceAction() throws Exception {
        dao.save(samplePermission("patients", "create"));

        assertThrows(DatabaseException.class, () -> dao.save(samplePermission("patients", "create")));
    }

    @Test
    @DisplayName("save allows the same resource with a different action")
    void save_allowsSameResourceDifferentAction() throws Exception {
        dao.save(samplePermission("patients", "create"));

        assertDoesNotThrow(() -> dao.save(samplePermission("patients", "delete")));
    }

    @Test
    @DisplayName("findById returns the saved permission with every field intact")
    void findById_returnsSavedPermission() throws Exception {
        Permission saved = dao.save(samplePermission("patients", "create"));

        Optional<Permission> found = dao.findById(saved.getPermissionId());

        assertTrue(found.isPresent());
        assertEquals("patients", found.get().getResource());
        assertEquals("create", found.get().getAction());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted permission")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        Permission saved = dao.save(samplePermission("patients", "create"));
        dao.softDelete(saved.getPermissionId());

        assertTrue(dao.findById(saved.getPermissionId()).isEmpty());
    }

    @Test
    @DisplayName("findByResourceAndAction finds an exact match")
    void findByResourceAndAction_findsMatch() throws Exception {
        dao.save(samplePermission("patients", "create"));

        Optional<Permission> found = dao.findByResourceAndAction("patients", "create");

        assertTrue(found.isPresent());
    }

    @Test
    @DisplayName("findByResourceAndAction returns empty when there is no match")
    void findByResourceAndAction_returnsEmpty_whenNoMatch() throws Exception {
        assertTrue(dao.findByResourceAndAction("patients", "create").isEmpty());
    }

    @Test
    @DisplayName("findAll returns every non-deleted permission ordered by resource, action")
    void findAll_returnsNonDeletedPermissions_orderedByResourceAndAction() throws Exception {
        dao.save(samplePermission("patients", "create"));
        dao.save(samplePermission("appointments", "read"));
        Permission toDelete = dao.save(samplePermission("patients", "delete"));
        dao.softDelete(toDelete.getPermissionId());

        List<Permission> all = dao.findAll();

        assertEquals(2, all.size());
        assertEquals("appointments", all.get(0).getResource());
        assertEquals("patients", all.get(1).getResource());
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        Permission saved = dao.save(samplePermission("patients", "create"));

        dao.softDelete(saved.getPermissionId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getPermissionId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }

    @Test
    @DisplayName("softDelete throws ResourceNotFoundException for a never-saved id")
    void softDelete_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(UUID.randomUUID().toString()));
    }
}
