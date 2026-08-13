package hospital.management.backend.utils;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.dao.auth.interfaces.RoleDAO;
import hospital.management.backend.dao.auth.interfaces.UserDAO;
import hospital.management.backend.dao.auth.interfaces.UserRoleDAO;
import hospital.management.backend.model.user.Role;
import hospital.management.backend.model.user.User;
import hospital.management.backend.model.user.UserRole;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public final class LoginLookupIndex {

    private static final AppLogger logger = AppLogger.getLogger(LoginLookupIndex.class);

    /** Page size used when paging the {@code users} table to build the snapshot. */
    private static final int REFRESH_PAGE_SIZE = 500;

    private static final LoginLookupIndex INSTANCE = new LoginLookupIndex();

    // Snapshot fields — swapped atomically at the end of refresh().
    private volatile List<User>                 usersByUsername  = List.of();
    private volatile Map<String, List<String>>  roleNamesByUserId = Map.of();

    private LoginLookupIndex() {}

    /** Returns the shared application instance. */
    public static LoginLookupIndex get() {
        return INSTANCE;
    }


    public synchronized void refresh(UserDAO userDAO, UserRoleDAO userRoleDAO, RoleDAO roleDAO) throws Exception {
        List<User> users = pageUsers(userDAO);
        if (users == null) return;
        //This sorts the list of users once by username in O(n log n) time.
        //bcz binary search require sorted values
        AlgorithmUtils.mergeSort(users, Comparator.comparing(User::getUsername));

        Map<String, String> roleNameById = new HashMap<>();
        List<Role> roles = roleDAO.findAll();
        if (roles != null) {
            for (Role role : roles) {
                roleNameById.put(role.getRoleId(), role.getRoleName());
            }
        }

        Map<String, List<String>> namesByUser = new HashMap<>();
        for (User user : users) {
            List<UserRole> assignments = userRoleDAO.findByUserId(user.getUserId());
            if (assignments == null || assignments.isEmpty()) continue;
            List<String> names = new ArrayList<>();
            for (UserRole assignment : assignments) {
                String name = roleNameById.get(assignment.getRoleId());
                if (name != null) names.add(name);
            }
            if (!names.isEmpty()) namesByUser.put(user.getUserId(), names);
        }

        this.usersByUsername  = List.copyOf(users);
        this.roleNamesByUserId = Map.copyOf(namesByUser);
        logger.info("Login index refreshed — " + users.size() + " users indexed.");
    }

    /** Drops the snapshot so the next login falls back to direct DAO lookups. */
    public synchronized void invalidate() {
        this.usersByUsername  = List.of();
        this.roleNamesByUserId = Map.of();
    }

    /**
     * Locates a user by username via binary search over the sorted snapshot.
     * O(log n) — no database call. Returns empty on a miss so the caller can
     * fall back to {@code UserDAO.findByUsername}.
     */
    public Optional<User> findUser(String username) {
        if (username == null) return Optional.empty();
        int idx = AlgorithmUtils.binarySearch(usersByUsername, username, User::getUsername);
        return idx >= 0 ? Optional.of(usersByUsername.get(idx)) : Optional.empty();
    }

    /**
     * Returns the user's active role names, in assignment order.
     * Returns empty when the user is not present in the snapshot.
     */
    public Optional<List<String>> findRoleNames(String userId) {
        if (userId == null) return Optional.empty();
        List<String> names = roleNamesByUserId.get(userId);
        return names == null ? Optional.empty() : Optional.of(names);
    }

    /**
     * Returns the user's primary (first) role name, mirroring the login flow's
     * "first active assignment" rule.
     */
    public Optional<String> findPrimaryRoleName(String userId) {
        return findRoleNames(userId)
                .flatMap(names -> names.isEmpty() ? Optional.empty() : Optional.of(names.get(0)));
    }

    /** Pages the {@code users} table (pageSize+1 trick detects a next page). Returns null on DAO failure. */
    private static List<User> pageUsers(UserDAO userDAO) throws Exception {
        List<User> users = new ArrayList<>();
        PageResult<User> page = userDAO.findAll(CursorPagination.firstPage(REFRESH_PAGE_SIZE));
        if (page == null) return null;
        users.addAll(page.getItems());
        while (page.hasMore() && page.getNextCursor() != null) {
            page = userDAO.findAll(CursorPagination.nextPage(page.getNextCursor(), REFRESH_PAGE_SIZE));
            if (page == null) return null;
            users.addAll(page.getItems());
        }
        return users;
    }
}
