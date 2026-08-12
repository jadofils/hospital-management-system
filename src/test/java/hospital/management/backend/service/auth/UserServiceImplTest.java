package hospital.management.backend.service.auth;

import hospital.management.backend.dao.auth.interfaces.UserDAO;
import hospital.management.backend.dto.auth.CreateUserDTO;
import hospital.management.backend.dto.auth.UpdateUserDTO;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Every id used here is a fresh random UUID rather than a fixed literal —
 * findById()/CacheService.get() reads through a real, JVM-wide, static L1
 * in-process cache with no reset hook exposed to tests, so a fixed id would
 * risk one test's cached DTO leaking into another test's assertions.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserDAO userDAO;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userDAO);
    }

    private User sampleUser(String id) {
        User user = new User();
        user.setUserId(id);
        user.setUsername("jane.doe");
        user.setEmail("jane.doe@example.com");
        user.setIsActive(true);
        return user;
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create throws IllegalArgumentException when username is blank")
    void create_throwsIllegalArgumentException_whenUsernameBlank() {
        CreateUserDTO dto = new CreateUserDTO(null, "  ", "password123", null);

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        verifyNoInteractions(userDAO);
    }

    @Test
    @DisplayName("create throws IllegalArgumentException when username is shorter than the minimum length")
    void create_throwsIllegalArgumentException_whenUsernameTooShort() {
        CreateUserDTO dto = new CreateUserDTO(null, "jd", "password123", null);

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        verifyNoInteractions(userDAO);
    }

    @Test
    @DisplayName("create throws IllegalArgumentException when password is shorter than the minimum length")
    void create_throwsIllegalArgumentException_whenPasswordTooShort() {
        CreateUserDTO dto = new CreateUserDTO(null, "jane.doe", "short", null);

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        verifyNoInteractions(userDAO);
    }

    @Test
    @DisplayName("create throws ValidationException when the username is already taken")
    void create_throwsValidationException_whenUsernameTaken() throws Exception {
        CreateUserDTO dto = new CreateUserDTO(null, "jane.doe", "password123", null);
        when(userDAO.existsByUsername("jane.doe")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.create(dto));
        verify(userDAO, never()).save(any());
    }

    @Test
    @DisplayName("create throws ValidationException when the email is already registered")
    void create_throwsValidationException_whenEmailAlreadyRegistered() throws Exception {
        CreateUserDTO dto = new CreateUserDTO(null, "jane.doe", "password123", "jane.doe@example.com");
        when(userDAO.existsByUsername("jane.doe")).thenReturn(false);
        when(userDAO.existsByEmail("jane.doe@example.com")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.create(dto));
        verify(userDAO, never()).save(any());
    }

    @Test
    @DisplayName("create saves a new user with a bcrypt password hash, never the raw password")
    void create_savesUser_withHashedPassword() throws Exception {
        CreateUserDTO dto = new CreateUserDTO(null, "jane.doe", "password123", "jane.doe@example.com");
        when(userDAO.existsByUsername("jane.doe")).thenReturn(false);
        when(userDAO.existsByEmail("jane.doe@example.com")).thenReturn(false);
        when(userDAO.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = service.create(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDAO).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("jane.doe", saved.getUsername());
        assertNotEquals("password123", saved.getPasswordHash());
        assertTrue(saved.getPasswordHash().startsWith("$2"));
        assertEquals("jane.doe", result.getUsername());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns a mapped DTO when the DAO finds a matching user")
    void findById_returnsMappedDto_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(userDAO.findById(id)).thenReturn(Optional.of(sampleUser(id)));

        UserDTO dto = service.findById(id);

        assertEquals(id, dto.getUserId());
        assertEquals("jane.doe", dto.getUsername());
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the DAO finds nothing")
    void findById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(userDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update throws ResourceNotFoundException when the user doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(userDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(new UpdateUserDTO(id, "new@example.com", null)));
    }

    @Test
    @DisplayName("update throws ValidationException when changing to an email already used by someone else")
    void update_throwsValidationException_whenEmailTakenBySomeoneElse() throws Exception {
        String id = UUID.randomUUID().toString();
        when(userDAO.findById(id)).thenReturn(Optional.of(sampleUser(id)));
        when(userDAO.existsByEmail("taken@example.com")).thenReturn(true);

        UpdateUserDTO dto = new UpdateUserDTO(id, "taken@example.com", null);

        assertThrows(ValidationException.class, () -> service.update(dto));
        verify(userDAO, never()).update(any());
    }

    @Test
    @DisplayName("update allows keeping the user's own current email unchanged without a conflict check")
    void update_allowsKeepingOwnEmail() throws Exception {
        String id = UUID.randomUUID().toString();
        when(userDAO.findById(id)).thenReturn(Optional.of(sampleUser(id))); // email = jane.doe@example.com
        when(userDAO.update(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserDTO dto = new UpdateUserDTO(id, "jane.doe@example.com", false);

        UserDTO result = service.update(dto);

        assertFalse(result.getIsActive());
        verify(userDAO, never()).existsByEmail(anyString());
    }

    // ── deactivate ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivate throws ResourceNotFoundException when the user doesn't exist")
    void deactivate_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(userDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deactivate(id));
    }

    @Test
    @DisplayName("deactivate flips isActive to false and persists the change")
    void deactivate_setsIsActiveFalse() throws Exception {
        String id = UUID.randomUUID().toString();
        User user = sampleUser(id);
        when(userDAO.findById(id)).thenReturn(Optional.of(user));

        service.deactivate(id);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDAO).update(captor.capture());
        assertFalse(captor.getValue().getIsActive());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete delegates to the DAO's soft-delete for the given id")
    void delete_delegatesToSoftDelete() throws Exception {
        String id = UUID.randomUUID().toString();

        service.delete(id);

        verify(userDAO).softDelete(id);
    }
}
