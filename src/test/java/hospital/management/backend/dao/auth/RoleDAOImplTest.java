package hospital.management.backend.dao.auth;

import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion here
 * runs RoleDAOImpl's actual SQL against a real database, proving the RETURNING clause,
 * gen_random_uuid() default, updated_at trigger, and the UNIQUE(role_name) constraint
 * all behave as the code assumes.
 */
class RoleDAOImplTest extends PostgresIntegrationTestBase {

    private final RoleDAOImpl dao = new RoleDAOImpl();

    private Role sampleRole(String name) {
        Role role = new Role();
        role.setRoleName(name);
        return role;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        Role saved = dao.save(sampleRole("Nurse"));

        assertNotNull(saved.getRoleId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getRoleId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save enforces the UNIQUE(role_name) constraint at the DB level")
    void save_enforcesUniqueRoleName() throws Exception {
        dao.save(sampleRole("Nurse"));

        assertThrows(DatabaseException.class, () -> dao.save(sampleRole("Nurse")));
    }

    @Test
    @DisplayName("findById returns the saved role with every field intact")
    void findById_returnsSavedRole() throws Exception {
        Role saved = dao.save(sampleRole("Nurse"));

        Optional<Role> found = dao.findById(saved.getRoleId());

        assertTrue(found.isPresent());
        assertEquals("Nurse", found.get().getRoleName());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted role")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        Role saved = dao.save(sampleRole("Nurse"));
        dao.softDelete(saved.getRoleId());

        assertTrue(dao.findById(saved.getRoleId()).isEmpty());
    }

    @Test
    @DisplayName("findByName finds a role by its exact name")
    void findByName_findsMatch() throws Exception {
        dao.save(sampleRole("Nurse"));

        Optional<Role> found = dao.findByName("Nurse");

        assertTrue(found.isPresent());
    }

    @Test
    @DisplayName("findByName returns empty when there is no match")
    void findByName_returnsEmpty_whenNoMatch() throws Exception {
        assertTrue(dao.findByName("Ghost").isEmpty());
    }

    @Test
    @DisplayName("findAll returns every non-deleted role ordered by role_name")
    void findAll_returnsNonDeletedRoles_orderedByName() throws Exception {
        dao.save(sampleRole("Nurse"));
        dao.save(sampleRole("Admin"));
        Role toDelete = dao.save(sampleRole("Zeta"));
        dao.softDelete(toDelete.getRoleId());

        List<Role> all = dao.findAll();

        assertEquals(2, all.size());
        assertEquals("Admin", all.get(0).getRoleName());
        assertEquals("Nurse", all.get(1).getRoleName());
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        Role saved = dao.save(sampleRole("Nurse"));

        dao.softDelete(saved.getRoleId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getRoleId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }

    @Test
    @DisplayName("softDelete throws ResourceNotFoundException for a never-saved id")
    void softDelete_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(UUID.randomUUID().toString()));
    }
}
