package hospital.management.backend.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class RoleNameTest {

    @ParameterizedTest
    @EnumSource(RoleName.class)
    @DisplayName("fromDbValue(getDbValue()) round-trips back to the same constant for every role")
    void fromDbValue_roundTripsForEveryConstant(RoleName role) {
        assertEquals(role, RoleName.fromDbValue(role.getDbValue()));
    }

    @Test
    @DisplayName("fromDbValue is case-insensitive")
    void fromDbValue_isCaseInsensitive() {
        assertEquals(RoleName.ADMIN, RoleName.fromDbValue("admin"));
        assertEquals(RoleName.ADMIN, RoleName.fromDbValue("ADMIN"));
        assertEquals(RoleName.ADMIN, RoleName.fromDbValue("AdMiN"));
    }

    @Test
    @DisplayName("fromDbValue throws IllegalArgumentException for an unknown role name")
    void fromDbValue_throwsOnUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> RoleName.fromDbValue("SuperAdmin"));
    }

    @Test
    @DisplayName("The five seeded roles all exist with the exact db values used by hospital_rbac_seed_postgresql.sql")
    void seededRoles_matchExpectedDbValues() {
        assertEquals("Admin", RoleName.ADMIN.getDbValue());
        assertEquals("Doctor", RoleName.DOCTOR.getDbValue());
        assertEquals("Receptionist", RoleName.RECEPTIONIST.getDbValue());
        assertEquals("Analyst", RoleName.ANALYST.getDbValue());
        assertEquals("Pharmacist", RoleName.PHARMACIST.getDbValue());
    }

    @Test
    @DisplayName("toString() returns the db value, not the Java constant name")
    void toString_returnsDbValue() {
        assertEquals("Admin", RoleName.ADMIN.toString());
    }
}
