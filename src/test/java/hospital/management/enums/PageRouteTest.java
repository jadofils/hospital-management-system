package hospital.management.enums;

import hospital.management.backend.model.enums.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class PageRouteTest {

    // ── isAllowedFor ──────────────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(RoleName.class)
    @DisplayName("A route with no configured roles (e.g. DASHBOARD) is allowed for every role")
    void unrestrictedRoute_allowsEveryRole(RoleName role) {
        assertTrue(PageRoute.DASHBOARD.isAllowedFor(role));
        assertTrue(PageRoute.PROFILE.isAllowedFor(role));
        assertTrue(PageRoute.HOME.isAllowedFor(role));
    }

    @Test
    @DisplayName("PATIENTS is allowed for Admin, Doctor, and Receptionist only")
    void patients_allowsAdminDoctorReceptionist() {
        assertTrue(PageRoute.PATIENTS.isAllowedFor(RoleName.ADMIN));
        assertTrue(PageRoute.PATIENTS.isAllowedFor(RoleName.DOCTOR));
        assertTrue(PageRoute.PATIENTS.isAllowedFor(RoleName.RECEPTIONIST));
        assertFalse(PageRoute.PATIENTS.isAllowedFor(RoleName.ANALYST));
        assertFalse(PageRoute.PATIENTS.isAllowedFor(RoleName.PHARMACIST));
    }

    @Test
    @DisplayName("USERS (admin console) is allowed for Admin only")
    void users_allowsAdminOnly() {
        assertTrue(PageRoute.USERS.isAllowedFor(RoleName.ADMIN));
        for (RoleName role : RoleName.values()) {
            if (role != RoleName.ADMIN) {
                assertFalse(PageRoute.USERS.isAllowedFor(role), role + " should not see Users management");
            }
        }
    }

    @Test
    @DisplayName("MY_SCHEDULE is allowed for Doctor only")
    void mySchedule_allowsDoctorOnly() {
        assertTrue(PageRoute.MY_SCHEDULE.isAllowedFor(RoleName.DOCTOR));
        assertFalse(PageRoute.MY_SCHEDULE.isAllowedFor(RoleName.ADMIN));
        assertFalse(PageRoute.MY_SCHEDULE.isAllowedFor(RoleName.RECEPTIONIST));
    }

    @Test
    @DisplayName("PHARMACY is allowed for Pharmacist and Admin only")
    void pharmacy_allowsPharmacistAndAdmin() {
        assertTrue(PageRoute.PHARMACY.isAllowedFor(RoleName.PHARMACIST));
        assertTrue(PageRoute.PHARMACY.isAllowedFor(RoleName.ADMIN));
        assertFalse(PageRoute.PHARMACY.isAllowedFor(RoleName.DOCTOR));
        assertFalse(PageRoute.PHARMACY.isAllowedFor(RoleName.RECEPTIONIST));
        assertFalse(PageRoute.PHARMACY.isAllowedFor(RoleName.ANALYST));
    }

    // ── fromKey ───────────────────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(PageRoute.class)
    @DisplayName("fromKey(getKey()) round-trips back to the same route for every route")
    void fromKey_roundTripsForEveryRoute(PageRoute route) {
        assertEquals(route, PageRoute.fromKey(route.getKey()));
    }

    @Test
    @DisplayName("fromKey is case-insensitive")
    void fromKey_isCaseInsensitive() {
        assertEquals(PageRoute.PATIENTS, PageRoute.fromKey("PATIENTS"));
        assertEquals(PageRoute.PATIENTS, PageRoute.fromKey("patients"));
    }

    @Test
    @DisplayName("fromKey throws IllegalArgumentException for an unknown key")
    void fromKey_throwsOnUnknownKey() {
        assertThrows(IllegalArgumentException.class, () -> PageRoute.fromKey("not-a-real-route"));
    }

    // ── Basic shape ───────────────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(PageRoute.class)
    @DisplayName("Every route has a non-blank key, label, and fxml path")
    void everyRoute_hasNonBlankFields(PageRoute route) {
        assertFalse(route.getKey().isBlank());
        assertFalse(route.getLabel().isBlank());
        assertFalse(route.getFxmlPath().isBlank());
        assertTrue(route.getFxmlPath().endsWith(".fxml"));
    }

    @Test
    @DisplayName("toString() returns the route's key")
    void toString_returnsKey() {
        assertEquals("patients", PageRoute.PATIENTS.toString());
    }
}
