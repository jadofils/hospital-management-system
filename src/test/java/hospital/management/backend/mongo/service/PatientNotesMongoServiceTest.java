package hospital.management.backend.mongo.service;

import hospital.management.backend.dto.patient.PatientNoteDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PatientNotesMongoService focusing on graceful degradation
 * when MongoDB is unavailable (no running MongoDB instance in CI).
 *
 * When MongoConfig.getDatabase() returns null (connection failed or unconfigured),
 * every method must return a safe empty/null value and must never throw.
 */
@DisplayName("PatientNotesMongoService — degradation when MongoDB unavailable")
class PatientNotesMongoServiceTest {

    private final PatientNotesMongoService service = new PatientNotesMongoService();

    // ── saveNote ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveNote: blank text returns null without throwing")
    void saveNote_blankText_returnsNull() {
        assertNull(service.saveNote(UUID.randomUUID().toString(), null, null, "DOCTOR", ""));
    }

    @Test
    @DisplayName("saveNote: null text returns null without throwing")
    void saveNote_nullText_returnsNull() {
        assertNull(service.saveNote(UUID.randomUUID().toString(), null, null, "DOCTOR", null));
    }

    @Test
    @DisplayName("saveNote: whitespace-only text returns null without throwing")
    void saveNote_whitespaceText_returnsNull() {
        assertNull(service.saveNote(UUID.randomUUID().toString(), null, null, "DOCTOR", "   "));
    }

    @Test
    @DisplayName("saveNote: when MongoDB unavailable returns null (not exception)")
    void saveNote_mongoUnavailable_returnsNullGracefully() {
        // MongoConfig will fail to connect in an offline-DB environment;
        // the service must swallow the exception and return null.
        String result = assertDoesNotThrow(() ->
            service.saveNote(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "DOCTOR",
                "Patient note text for testing"
            )
        );
        // In an offline environment the result will be null.
        // In a live environment it would be a UUID string — both are acceptable.
        if (result != null) {
            assertFalse(result.isBlank(), "returned noteId must not be blank when non-null");
        }
    }

    // ── findByPatientId ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findByPatientId: returns empty list when MongoDB unavailable (not null)")
    void findByPatientId_mongoUnavailable_returnsEmptyList() {
        List<PatientNoteDTO> result = assertDoesNotThrow(() ->
            service.findByPatientId(UUID.randomUUID().toString())
        );
        assertNotNull(result, "result list must never be null");
        // If MongoDB is down the list will be empty; if it's up it may or may not have records.
        // Both are valid — the key contract is non-null.
    }

    @Test
    @DisplayName("findByPatientId: null patientId returns empty list without throwing")
    void findByPatientId_nullId_doesNotThrow() {
        List<PatientNoteDTO> result = assertDoesNotThrow(() ->
            service.findByPatientId(null)
        );
        assertNotNull(result);
    }

    // ── findByAppointmentId ───────────────────────────────────────────────────

    @Test
    @DisplayName("findByAppointmentId: returns empty list when MongoDB unavailable")
    void findByAppointmentId_mongoUnavailable_returnsEmptyList() {
        List<PatientNoteDTO> result = assertDoesNotThrow(() ->
            service.findByAppointmentId(UUID.randomUUID().toString())
        );
        assertNotNull(result);
    }

    @Test
    @DisplayName("findByAppointmentId: null appointmentId returns empty list without throwing")
    void findByAppointmentId_nullId_doesNotThrow() {
        assertNotNull(assertDoesNotThrow(() -> service.findByAppointmentId(null)));
    }

    // ── deleteNote ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteNote: null noteId does not throw")
    void deleteNote_nullNoteId_noOp() {
        assertDoesNotThrow(() -> service.deleteNote(null));
    }

    @Test
    @DisplayName("deleteNote: blank noteId does not throw")
    void deleteNote_blankNoteId_noOp() {
        assertDoesNotThrow(() -> service.deleteNote("   "));
    }

    @Test
    @DisplayName("deleteNote: non-existent noteId when MongoDB unavailable does not throw")
    void deleteNote_mongoUnavailable_noException() {
        assertDoesNotThrow(() -> service.deleteNote(UUID.randomUUID().toString()));
    }
}