package hospital.management.backend.dao.auth;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.user.User;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion here
 * runs UserDAOImpl's actual SQL against a real database, proving the RETURNING clause,
 * gen_random_uuid() default, updated_at trigger, and the UNIQUE(username)/UNIQUE(email)
 * constraints all behave as the code assumes.
 */
class UserDAOImplTest extends PostgresIntegrationTestBase {

    private final UserDAOImpl dao = new UserDAOImpl();

    private User sampleUser(String username, String email) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash("hashed-password");
        u.setEmail(email);
        u.setIsActive(true);
        return u;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        User saved = dao.save(sampleUser("jane.doe", "jane.doe@example.com"));

        assertNotNull(saved.getUserId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getUserId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save enforces the UNIQUE(username) constraint at the DB level")
    void save_enforcesUniqueUsername() throws Exception {
        dao.save(sampleUser("jane.doe", "jane.doe@example.com"));

        assertThrows(DatabaseException.class, () -> dao.save(sampleUser("jane.doe", "other@example.com")));
    }

    @Test
    @DisplayName("save enforces the UNIQUE(email) constraint at the DB level — unlike patients.email, "
            + "users.email IS declared UNIQUE in the schema")
    void save_enforcesUniqueEmail() throws Exception {
        dao.save(sampleUser("jane.doe", "shared@example.com"));

        assertThrows(DatabaseException.class, () -> dao.save(sampleUser("john.smith", "shared@example.com")));
    }

    @Test
    @DisplayName("save allows two users with a NULL email — Postgres treats NULLs as distinct under UNIQUE")
    void save_allowsMultipleNullEmails() throws Exception {
        dao.save(sampleUser("jane.doe", null));

        assertDoesNotThrow(() -> dao.save(sampleUser("john.smith", null)));
    }

    @Test
    @DisplayName("save via the caller-supplied Connection overload composes into a borrowed connection")
    void save_viaConnectionOverload() throws Exception {
        User user = sampleUser("jane.doe", "jane.doe@example.com");
        try (Connection conn = DBConnection.getConnection()) {
            dao.save(user, conn);
        }

        assertNotNull(user.getUserId());
        assertTrue(dao.findById(user.getUserId()).isPresent());
    }

    @Test
    @DisplayName("findById returns the saved user with every field intact")
    void findById_returnsSavedUser() throws Exception {
        User saved = dao.save(sampleUser("jane.doe", "jane.doe@example.com"));

        Optional<User> found = dao.findById(saved.getUserId());

        assertTrue(found.isPresent());
        assertEquals("jane.doe", found.get().getUsername());
        assertEquals("jane.doe@example.com", found.get().getEmail());
        assertTrue(found.get().getIsActive());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted user")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        User saved = dao.save(sampleUser("jane.doe", "jane.doe@example.com"));
        dao.softDelete(saved.getUserId());

        assertTrue(dao.findById(saved.getUserId()).isEmpty());
    }

    @Test
    @DisplayName("findByUsername finds a user by their exact username")
    void findByUsername_findsMatch() throws Exception {
        dao.save(sampleUser("jane.doe", "jane.doe@example.com"));

        assertTrue(dao.findByUsername("jane.doe").isPresent());
    }

    @Test
    @DisplayName("findByEmail finds a user by their exact email")
    void findByEmail_findsMatch() throws Exception {
        dao.save(sampleUser("jane.doe", "jane.doe@example.com"));

        assertTrue(dao.findByEmail("jane.doe@example.com").isPresent());
    }

    @Test
    @DisplayName("findAll returns every non-deleted user, most-recently-created first")
    void findAll_returnsNonDeletedUsers() throws Exception {
        dao.save(sampleUser("jane.doe", "jane.doe@example.com"));
        User toDelete = dao.save(sampleUser("deleted.user", "deleted@example.com"));
        dao.softDelete(toDelete.getUserId());

        PageResult<User> page = dao.findAll(CursorPagination.firstPage());

        assertEquals(1, page.getCount());
        assertEquals("jane.doe", page.getItems().get(0).getUsername());
    }

    @Test
    @DisplayName("update persists the email/isActive change and refreshes updated_at, "
            + "but does NOT touch username (the UPDATE statement only SETs email and is_active)")
    void update_persistsEmailAndIsActive_leavesUsernameUnchanged() throws Exception {
        User saved = dao.save(sampleUser("jane.doe", "jane.doe@example.com"));

        saved.setEmail("new.email@example.com");
        saved.setIsActive(false);
        saved.setUsername("attempted.rename");
        User updated = dao.update(saved);

        assertEquals("new.email@example.com", updated.getEmail());
        assertFalse(updated.getIsActive());
        Optional<User> reloaded = dao.findById(saved.getUserId());
        assertEquals("new.email@example.com", reloaded.get().getEmail());
        assertFalse(reloaded.get().getIsActive());
        assertEquals("jane.doe", reloaded.get().getUsername(),
                "update() has no SET username clause, so a changed in-memory username is silently discarded");
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException for a user id that doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() {
        User ghost = sampleUser("ghost", "ghost@example.com");
        ghost.setUserId(UUID.randomUUID().toString());

        assertThrows(ResourceNotFoundException.class, () -> dao.update(ghost));
    }

    @Test
    @DisplayName("update via the caller-supplied Connection overload composes into a borrowed connection")
    void update_viaConnectionOverload() throws Exception {
        User saved = dao.save(sampleUser("jane.doe", "jane.doe@example.com"));
        saved.setEmail("via.conn@example.com");

        try (Connection conn = DBConnection.getConnection()) {
            dao.update(saved, conn);
        }

        assertEquals("via.conn@example.com", dao.findById(saved.getUserId()).get().getEmail());
    }

    @Test
    @DisplayName("updatePasswordHash changes only the password hash")
    void updatePasswordHash_changesHash() throws Exception {
        User saved = dao.save(sampleUser("jane.doe", "jane.doe@example.com"));

        dao.updatePasswordHash(saved.getUserId(), "new-hashed-password");

        Optional<User> reloaded = dao.findById(saved.getUserId());
        assertEquals("new-hashed-password", reloaded.get().getPasswordHash());
        assertEquals("jane.doe", reloaded.get().getUsername());
    }

    @Test
    @DisplayName("updatePasswordHash throws ResourceNotFoundException for a user id that doesn't exist")
    void updatePasswordHash_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class,
                () -> dao.updatePasswordHash(UUID.randomUUID().toString(), "new-hash"));
    }

    @Test
    @DisplayName("updatePasswordHash via the caller-supplied Connection overload composes into a borrowed connection")
    void updatePasswordHash_viaConnectionOverload() throws Exception {
        User saved = dao.save(sampleUser("jane.doe", "jane.doe@example.com"));

        try (Connection conn = DBConnection.getConnection()) {
            dao.updatePasswordHash(saved.getUserId(), "conn-hash", conn);
        }

        assertEquals("conn-hash", dao.findById(saved.getUserId()).get().getPasswordHash());
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        User saved = dao.save(sampleUser("jane.doe", "jane.doe@example.com"));

        dao.softDelete(saved.getUserId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getUserId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }

    @Test
    @DisplayName("existsByUsername returns true only for a currently-saved username")
    void existsByUsername_reflectsSavedState() throws Exception {
        assertFalse(dao.existsByUsername("jane.doe"));

        dao.save(sampleUser("jane.doe", "jane.doe@example.com"));

        assertTrue(dao.existsByUsername("jane.doe"));
    }

    @Test
    @DisplayName("existsByEmail returns true only for a currently-saved email")
    void existsByEmail_reflectsSavedState() throws Exception {
        assertFalse(dao.existsByEmail("jane.doe@example.com"));

        dao.save(sampleUser("jane.doe", "jane.doe@example.com"));

        assertTrue(dao.existsByEmail("jane.doe@example.com"));
    }
}
