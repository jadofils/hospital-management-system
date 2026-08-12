package hospital.management.backend.mapper.patient;

import hospital.management.backend.dto.patient.CreatePatientDTO;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.patient.PatientSummaryDTO;
import hospital.management.backend.model.patient.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PatientMapperTest {

    private Patient sample() {
        return new Patient(
                "patient-1", "Jane", "Doe",
                LocalDate.of(1990, 5, 20), "Female", "+15558675309",
                "jane.doe@example.com", "123 Main St", "active",
                LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 2, 9, 0), null);
    }

    // ── toDTO ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toDTO copies every field from the entity")
    void toDTO_copiesAllFields() {
        PatientDTO dto = PatientMapper.toDTO(sample());

        assertEquals("patient-1", dto.getPatientId());
        assertEquals("Jane", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals(LocalDate.of(1990, 5, 20), dto.getDob());
        assertEquals("Female", dto.getGender());
        assertEquals("+15558675309", dto.getPhone());
        assertEquals("jane.doe@example.com", dto.getEmail());
        assertEquals("123 Main St", dto.getAddress());
        assertEquals("active", dto.getStatus());
        assertEquals(LocalDateTime.of(2026, 1, 1, 9, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 1, 2, 9, 0), dto.getUpdatedAt());
    }

    @Test
    @DisplayName("toDTO returns null for a null entity instead of throwing")
    void toDTO_nullSafe() {
        assertNull(PatientMapper.toDTO(null));
    }

    // ── toSummaryDTO ──────────────────────────────────────────────────────

    @Test
    @DisplayName("toSummaryDTO combines first/last name into a full name and carries id/gender/phone/email")
    void toSummaryDTO_combinesFullName() {
        PatientSummaryDTO summary = PatientMapper.toSummaryDTO(sample());

        assertEquals("patient-1", summary.getPatientId());
        assertEquals("Jane Doe", summary.getFullName());
        assertEquals("Female", summary.getGender());
        assertEquals("+15558675309", summary.getPhone());
        assertEquals("jane.doe@example.com", summary.getEmail());
        assertEquals("active", summary.getStatus());
    }

    @Test
    @DisplayName("toSummaryDTO returns null for a null entity instead of throwing")
    void toSummaryDTO_nullSafe() {
        assertNull(PatientMapper.toSummaryDTO(null));
    }

    // ── toEntity ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("toEntity copies every creation field but leaves the id/timestamps unset")
    void toEntity_copiesCreationFieldsOnly() {
        CreatePatientDTO dto = new CreatePatientDTO(
                "John", "Smith", LocalDate.of(1985, 3, 15),
                "Male", "+15551234567", "john.smith@example.com", "456 Oak Ave");

        Patient entity = PatientMapper.toEntity(dto);

        assertNull(entity.getPatientId(), "a freshly mapped entity has no id yet — assigned by the caller/DAO");
        assertEquals("John", entity.getFirstName());
        assertEquals("Smith", entity.getLastName());
        assertEquals(LocalDate.of(1985, 3, 15), entity.getDob());
        assertEquals("M", entity.getGender());
        assertEquals("+15551234567", entity.getPhone());
        assertEquals("john.smith@example.com", entity.getEmail());
        assertEquals("456 Oak Ave", entity.getAddress());
    }

    @Test
    @DisplayName("toEntity returns null for a null DTO instead of throwing")
    void toEntity_nullSafe() {
        assertNull(PatientMapper.toEntity(null));
    }

    // ── Polymorphic behaviour inherited from Person ──────────────────────

    @Test
    @DisplayName("getSummary() uses the Patient-specific display title via polymorphism")
    void getSummary_usesPatientDisplayTitle() {
        Patient patient = sample();
        assertEquals("Patient", patient.getDisplayTitle());
        assertEquals("Patient Jane Doe | jane.doe@example.com", patient.getSummary());
    }
}
