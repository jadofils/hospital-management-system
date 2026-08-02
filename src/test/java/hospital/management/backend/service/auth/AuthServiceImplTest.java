package hospital.management.backend.service.auth;

import hospital.management.backend.config.db.TransactionManager;
import hospital.management.backend.config.security.PasswordConfig;
import hospital.management.backend.dao.auth.interfaces.RoleDAO;
import hospital.management.backend.dao.auth.interfaces.UserDAO;
import hospital.management.backend.dao.auth.interfaces.UserRoleDAO;
import hospital.management.backend.dao.auth.interfaces.UserSessionDAO;
import hospital.management.backend.dao.log.interfaces.AuditLogDAO;
import hospital.management.backend.dto.auth.LoginRequestDTO;
import hospital.management.backend.dto.auth.LoginResponseDTO;
import hospital.management.backend.exceptions.AuthException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.user.Role;
import hospital.management.backend.model.user.User;
import hospital.management.backend.model.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * login()/logout()/changePassword() each wrap their DAO writes in
 * TransactionManager.executeInTransaction(...), a static method that normally
 * opens a real JDBC Connection — mocked here (via mockStatic) to just invoke
 * the work lambda directly with a stub Connection, so these tests never touch
 * a real database. EventBus.publish(...) is left un-mocked: with no listeners
 * registered in this test process it's a guaranteed no-op (see EventBus.publish's
 * early-return when the listener list is null/empty).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserDAO userDAO;
    @Mock private UserSessionDAO userSessionDAO;
    @Mock private UserRoleDAO userRoleDAO;
    @Mock private RoleDAO roleDAO;
    @Mock private AuditLogDAO auditLogDAO;

    private AuthServiceImpl service;
    private MockedStatic<TransactionManager> transactionManagerMock;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userDAO, userSessionDAO, userRoleDAO, roleDAO, auditLogDAO);

        transactionManagerMock = mockStatic(TransactionManager.class);
        transactionManagerMock
                .when(() -> TransactionManager.executeInTransaction(any(TransactionManager.VoidTransactionalWork.class)))
                .thenAnswer(invocation -> {
                    TransactionManager.VoidTransactionalWork work = invocation.getArgument(0);
                    work.execute(mock(Connection.class));
                    return null;
                });
    }

    @AfterEach
    void tearDown() {
        transactionManagerMock.close();
    }

    private User activeUser(String userId, String plainPassword) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername("jane.doe");
        user.setPasswordHash(PasswordConfig.hash(plainPassword));
        user.setEmail("jane.doe@example.com");
        user.setIsActive(true);
        return user;
    }

    // ── login ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login throws IllegalArgumentException when username is blank")
    void login_throwsIllegalArgumentException_whenUsernameBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> service.login(new LoginRequestDTO("  ", "password")));
        verifyNoInteractions(userDAO);
    }

    @Test
    @DisplayName("login throws AuthException with a generic message when the username doesn't exist")
    void login_throwsAuthException_whenUsernameNotFound() throws Exception {
        when(userDAO.findByUsername("ghost")).thenReturn(Optional.empty());

        AuthException ex = assertThrows(AuthException.class,
                () -> service.login(new LoginRequestDTO("ghost", "password")));
        assertEquals("Invalid username or password.", ex.getMessage());
    }

    @Test
    @DisplayName("login throws AuthException when the account has been deactivated")
    void login_throwsAuthException_whenAccountDeactivated() throws Exception {
        User user = activeUser(UUID.randomUUID().toString(), "correct-password");
        user.setIsActive(false);
        when(userDAO.findByUsername("jane.doe")).thenReturn(Optional.of(user));

        AuthException ex = assertThrows(AuthException.class,
                () -> service.login(new LoginRequestDTO("jane.doe", "correct-password")));
        assertTrue(ex.getMessage().toLowerCase().contains("deactivated"));
        verifyNoInteractions(userRoleDAO);
    }

    @Test
    @DisplayName("login throws AuthException with the same generic message when the password is wrong "
            + "(never reveals whether the username or the password was the problem)")
    void login_throwsAuthException_whenPasswordWrong() throws Exception {
        User user = activeUser(UUID.randomUUID().toString(), "correct-password");
        when(userDAO.findByUsername("jane.doe")).thenReturn(Optional.of(user));

        AuthException ex = assertThrows(AuthException.class,
                () -> service.login(new LoginRequestDTO("jane.doe", "wrong-password")));
        assertEquals("Invalid username or password.", ex.getMessage());
    }

    @Test
    @DisplayName("login throws AuthException when the user has no assigned role")
    void login_throwsAuthException_whenNoRoleAssigned() throws Exception {
        String userId = UUID.randomUUID().toString();
        User user = activeUser(userId, "correct-password");
        when(userDAO.findByUsername("jane.doe")).thenReturn(Optional.of(user));
        when(userRoleDAO.findByUserId(userId)).thenReturn(List.of());

        AuthException ex = assertThrows(AuthException.class,
                () -> service.login(new LoginRequestDTO("jane.doe", "correct-password")));
        assertTrue(ex.getMessage().toLowerCase().contains("role"));
    }

    @Test
    @DisplayName("login throws AuthException when the assigned role no longer exists")
    void login_throwsAuthException_whenAssignedRoleMissing() throws Exception {
        String userId = UUID.randomUUID().toString();
        User user = activeUser(userId, "correct-password");
        when(userDAO.findByUsername("jane.doe")).thenReturn(Optional.of(user));
        when(userRoleDAO.findByUserId(userId)).thenReturn(List.of(new UserRole(userId, "role-1", null, null, null)));
        when(roleDAO.findById("role-1")).thenReturn(Optional.empty());

        assertThrows(AuthException.class,
                () -> service.login(new LoginRequestDTO("jane.doe", "correct-password")));
    }

    @Test
    @DisplayName("login succeeds for a valid active user and returns a token embedding their primary role")
    void login_succeeds_returnsTokenAndRole() throws Exception {
        String userId = UUID.randomUUID().toString();
        User user = activeUser(userId, "correct-password");
        when(userDAO.findByUsername("jane.doe")).thenReturn(Optional.of(user));
        when(userRoleDAO.findByUserId(userId)).thenReturn(List.of(new UserRole(userId, "role-1", null, null, null)));
        when(roleDAO.findById("role-1")).thenReturn(Optional.of(new Role("role-1", "Doctor", null, null, null)));

        LoginResponseDTO response = service.login(new LoginRequestDTO("jane.doe", "correct-password"));

        assertNotNull(response.getToken());
        assertEquals(userId, response.getUserId());
        assertEquals("jane.doe", response.getUsername());
        assertEquals("Doctor", response.getRole());
        verify(userSessionDAO).save(any(), any(Connection.class));
        verify(auditLogDAO).save(any(), any(Connection.class));
    }

    // ── changePassword ────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword throws IllegalArgumentException when the new password is too short")
    void changePassword_throwsIllegalArgumentException_whenNewPasswordTooShort() {
        assertThrows(IllegalArgumentException.class,
                () -> service.changePassword("user-1", "old-password", "short"));
        verifyNoInteractions(userDAO);
    }

    @Test
    @DisplayName("changePassword throws ResourceNotFoundException when the user doesn't exist")
    void changePassword_throwsResourceNotFoundException_whenUserMissing() throws Exception {
        when(userDAO.findById("missing-user")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.changePassword("missing-user", "old-password", "new-password-123"));
    }

    @Test
    @DisplayName("changePassword throws AuthException when the current password is wrong")
    void changePassword_throwsAuthException_whenOldPasswordWrong() throws Exception {
        String userId = UUID.randomUUID().toString();
        User user = activeUser(userId, "correct-old-password");
        when(userDAO.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(AuthException.class,
                () -> service.changePassword(userId, "wrong-old-password", "new-password-123"));
        verify(userDAO, never()).updatePasswordHash(anyString(), anyString(), any(Connection.class));
    }

    @Test
    @DisplayName("changePassword updates the password hash when the current password is correct")
    void changePassword_updatesHash_whenOldPasswordCorrect() throws Exception {
        String userId = UUID.randomUUID().toString();
        User user = activeUser(userId, "correct-old-password");
        when(userDAO.findById(userId)).thenReturn(Optional.of(user));

        service.changePassword(userId, "correct-old-password", "new-password-123");

        verify(userDAO).updatePasswordHash(eq(userId), anyString(), any(Connection.class));
    }
}
