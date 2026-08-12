package hospital.management.backend.service.auth;

import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.config.EnvConfig;
import hospital.management.backend.config.db.TransactionManager;
import hospital.management.backend.config.security.JwtConfig;
import hospital.management.backend.config.security.PasswordConfig;
import hospital.management.backend.dao.auth.interfaces.RoleDAO;
import hospital.management.backend.dao.auth.interfaces.UserDAO;
import hospital.management.backend.dao.auth.interfaces.UserRoleDAO;
import hospital.management.backend.dao.auth.interfaces.UserSessionDAO;
import hospital.management.backend.dao.log.interfaces.AuditLogDAO;
import hospital.management.backend.dto.auth.LoginRequestDTO;
import hospital.management.backend.dto.auth.LoginResponseDTO;
import hospital.management.backend.dto.auth.UserSessionDTO;
import hospital.management.backend.exceptions.AuthException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.mapper.auth.UserSessionMapper;
import hospital.management.backend.model.user.AuditLog;
import hospital.management.backend.model.user.Role;
import hospital.management.backend.model.user.User;
import hospital.management.backend.model.user.UserRole;
import hospital.management.backend.model.user.UserSession;
import hospital.management.backend.service.auth.interfaces.AuthService;
import hospital.management.backend.utils.LoginLookupIndex;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the login/logout/change-password flow: verifies credentials against
 * the DB (never trusts the caller), and treats "open a session" and "record
 * the audit trail entry" as one atomic unit of work via TransactionManager —
 * a login is never recorded as a session with no audit trail, or vice versa.
 */
public class AuthServiceImpl implements AuthService {

    private final UserDAO        userDAO;
    private final UserSessionDAO userSessionDAO;
    private final UserRoleDAO    userRoleDAO;
    private final RoleDAO        roleDAO;
    private final AuditLogDAO    auditLogDAO;

    public AuthServiceImpl(UserDAO userDAO, UserSessionDAO userSessionDAO,
                            UserRoleDAO userRoleDAO, RoleDAO roleDAO, AuditLogDAO auditLogDAO) {
        this.userDAO        = userDAO;
        this.userSessionDAO = userSessionDAO;
        this.userRoleDAO    = userRoleDAO;
        this.roleDAO        = roleDAO;
        this.auditLogDAO    = auditLogDAO;
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) throws Exception {
        String username = ValidatorUtils.requireNonBlank(request.getUsername(), "username");
        String password = ValidatorUtils.requireNonBlank(request.getPassword(), "password");

        // Fast path: the login screen warms an in-memory index (sorted by username,
        // searched via binary search) so login skips the DB lookup entirely. The
        // index is only a cache — any miss falls through to the DAO, which remains
        // the source of truth, so a stale/empty index can never block a login.
        LoginLookupIndex index = LoginLookupIndex.get();
        User user = index.findUser(username).orElse(null);
        if (user == null) {
            user = userDAO.findByUsername(username)
                    .orElseThrow(() -> new AuthException("Invalid username or password."));
        }
        final User account = user; // effectively-final snapshot for the transaction lambda below

        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new AuthException("This account has been deactivated.");
        }
        if (!PasswordConfig.verify(password, account.getPasswordHash())) {
            throw new AuthException("Invalid username or password.");
        }

        // RBAC model supports multiple roles per user — the token carries every
        // active role name plus a "primary" one (the first active assignment) for
        // code that only needs a single display/audit role.
        List<String> roleNames = index.findRoleNames(account.getUserId()).orElse(null);
        if (roleNames == null) {
            List<UserRole> assignments = userRoleDAO.findByUserId(account.getUserId());
            if (assignments.isEmpty()) {
                throw new AuthException("This account has no assigned role. Contact an administrator.");
            }
            roleNames = new ArrayList<>();
            for (UserRole assignment : assignments) {
                Role role = roleDAO.findById(assignment.getRoleId())
                        .orElseThrow(() -> new AuthException("Assigned role no longer exists."));
                roleNames.add(role.getRoleName());
            }
        }
        if (roleNames.isEmpty()) {
            throw new AuthException("This account has no assigned role. Contact an administrator.");
        }
        String primaryRoleName = roleNames.get(0);

        String token = JwtConfig.generateToken(account.getUserId(), account.getUsername(), primaryRoleName, roleNames);
        UserSession session = new UserSession();
        session.setUserId(account.getUserId());
        session.setExpiresAt(LocalDateTime.now().plusHours(EnvConfig.getJwtExpiryHours()));
        session.setIsActive(true);

        // Opening the session and recording the audit entry succeed or fail together.
        TransactionManager.executeInTransaction(conn -> {
            userSessionDAO.save(session, conn);

            AuditLog log = new AuditLog();
            log.setUserId(account.getUserId());
            log.setAction("LOGIN");
            log.setTableAffected("users");
            log.setRecordId(account.getUserId());
            auditLogDAO.save(log, conn);
        });

        EventBus.publish(AppEventType.USER_LOGGED_IN, account.getUserId());
        return new LoginResponseDTO(token, session.getSessionId(), account.getUserId(), account.getUsername(), primaryRoleName, roleNames);
    }

    @Override
    public void logout(String sessionId) throws Exception {
        ValidatorUtils.requireNonBlank(sessionId, "sessionId");
        UserSession session = userSessionDAO.findById(sessionId).orElse(null);

        CacheService.evict(CacheKey.session(sessionId));
        TransactionManager.executeInTransaction(conn -> {
            userSessionDAO.deactivate(sessionId, conn);
            if (session != null) {
                AuditLog log = new AuditLog();
                log.setUserId(session.getUserId());
                log.setAction("LOGOUT");
                log.setTableAffected("users");
                log.setRecordId(session.getUserId());
                auditLogDAO.save(log, conn);
            }
        });
        if (session != null) EventBus.publish(AppEventType.USER_LOGGED_OUT, session.getUserId());
    }

    @Override
    public void logoutAllSessions(String userId) throws Exception {
        ValidatorUtils.requireNonBlank(userId, "userId");
        CacheService.evictByPattern(CacheKey.ALL_SESSIONS);
        TransactionManager.executeInTransaction(conn -> {
            userSessionDAO.deactivateAll(userId, conn);

            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setAction("LOGOUT_ALL");
            log.setTableAffected("users");
            log.setRecordId(userId);
            auditLogDAO.save(log, conn);
        });
        EventBus.publish(AppEventType.USER_LOGGED_OUT, userId);
    }

    @Override
    public List<UserSessionDTO> findActiveSessions(String userId) throws Exception {
        List<UserSessionDTO> dtos = new ArrayList<>();
        for (UserSession session : userSessionDAO.findActiveByUserId(userId)) {
            dtos.add(UserSessionMapper.toDTO(session));
        }
        return dtos;
    }

    @Override
    public void changePassword(String userId, String oldPassword, String newPassword) throws Exception {
        ValidatorUtils.requireNonBlank(oldPassword, "oldPassword");
        ValidatorUtils.requireNonBlank(newPassword, "newPassword");
        ValidatorUtils.requireMinLength(newPassword, 8, "newPassword");

        User user = userDAO.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!PasswordConfig.verify(oldPassword, user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect.");
        }

        String newHash = PasswordConfig.hash(newPassword);
        CacheService.evict(CacheKey.user(userId));
        LoginLookupIndex.get().invalidate();
        TransactionManager.executeInTransaction(conn -> {
            userDAO.updatePasswordHash(userId, newHash, conn);

            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setAction("PASSWORD_CHANGE");
            log.setTableAffected("users");
            log.setRecordId(userId);
            auditLogDAO.save(log, conn);
        });
        EventBus.publish(AppEventType.USER_UPDATED, userId);
    }

    @Override
    public String resetPasswordByEmail(String email) throws Exception {
        ValidatorUtils.requireNonBlank(email, "email");
        User user = userDAO.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User", email));
        // Generate a temporary password (8 chars alphanumeric) — admin can force change later
        String temp = java.util.UUID.randomUUID().toString().replaceAll("[^A-Za-z0-9]", "").substring(0, 10);
        String newHash = PasswordConfig.hash(temp);

        CacheService.evict(CacheKey.user(user.getUserId()));
        LoginLookupIndex.get().invalidate();
        TransactionManager.executeInTransaction(conn -> {
            userDAO.updatePasswordHash(user.getUserId(), newHash, conn);

            AuditLog log = new AuditLog();
            log.setUserId(user.getUserId());
            log.setAction("PASSWORD_RESET");
            log.setTableAffected("users");
            log.setRecordId(user.getUserId());
            auditLogDAO.save(log, conn);
        });

        EventBus.publish(AppEventType.USER_UPDATED, user.getUserId());
        return temp;
    }
}
