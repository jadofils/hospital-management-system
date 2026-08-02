package hospital.management.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTypeTest {

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    @DisplayName("toastCssClass() always prefixes the css class with 'toast-'")
    void toastCssClass_prefixesWithToast(NotificationType type) {
        assertEquals("toast-" + type.getCssClass(), type.toastCssClass());
    }

    @Test
    @DisplayName("Each constant has its expected css class and label")
    void constants_haveExpectedValues() {
        assertEquals("info", NotificationType.INFO.getCssClass());
        assertEquals("Info", NotificationType.INFO.getLabel());
        assertEquals("toast-success", NotificationType.SUCCESS.toastCssClass());
        assertEquals("toast-warning", NotificationType.WARNING.toastCssClass());
        assertEquals("toast-error", NotificationType.ERROR.toastCssClass());
    }

    @Test
    @DisplayName("toString() returns the raw css class, not the toast- prefixed form")
    void toString_returnsRawCssClass() {
        assertEquals("error", NotificationType.ERROR.toString());
    }

    @Test
    @DisplayName("Exactly four severities are defined")
    void exactlyFourSeverities() {
        assertEquals(4, NotificationType.values().length);
    }
}
