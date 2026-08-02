package hospital.management.backend.mapper.doctor;

import hospital.management.backend.dto.doctor.CreateDoctorDTO;
import hospital.management.backend.dto.doctor.DoctorDTO;
import hospital.management.backend.dto.doctor.DoctorSummaryDTO;
import hospital.management.backend.model.doctor.Doctor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DoctorMapperTest {

    private Doctor sample() {
        return new Doctor(
                "doctor-1", "dept-1", "Sarah", "Chen", "Cardiology",
                "+15551112222", "sarah.chen@example.com",
                LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 2, 9, 0), null);
    }

    @Test
    @DisplayName("toDTO copies every field, including the department reference")
    void toDTO_copiesAllFields() {
        DoctorDTO dto = DoctorMapper.toDTO(sample());

        assertEquals("doctor-1", dto.getDoctorId());
        assertEquals("dept-1", dto.getDepartmentId());
        assertEquals("Sarah", dto.getFirstName());
        assertEquals("Chen", dto.getLastName());
        assertEquals("Cardiology", dto.getSpecialization());
        assertEquals("+15551112222", dto.getPhone());
        assertEquals("sarah.chen@example.com", dto.getEmail());
        assertEquals(LocalDateTime.of(2026, 1, 1, 9, 0), dto.getCreatedAt());
    }

    @Test
    @DisplayName("toDTO returns null for a null entity instead of throwing")
    void toDTO_nullSafe() {
        assertNull(DoctorMapper.toDTO(null));
    }

    @Test
    @DisplayName("DoctorDTO.getFullName() prefixes 'Dr.' unlike the plain Person full name")
    void doctorDTO_fullNameHasDrPrefix() {
        DoctorDTO dto = DoctorMapper.toDTO(sample());
        assertEquals("Dr. Sarah Chen", dto.getFullName());
    }

    @Test
    @DisplayName("toSummaryDTO carries the given department name through, decoupled from departmentId")
    void toSummaryDTO_carriesDepartmentName() {
        DoctorSummaryDTO summary = DoctorMapper.toSummaryDTO(sample(), "Cardiology Department");

        assertEquals("doctor-1", summary.getDoctorId());
        // Uses the entity's plain Person.getFullName() ("Sarah Chen") — the "Dr." prefix is
        // only added by DoctorDTO's own getFullName() override, a separate class/method.
        assertEquals("Sarah Chen", summary.getFullName());
        assertEquals("Cardiology", summary.getSpecialization());
        assertEquals("Cardiology Department", summary.getDepartmentName());
    }

    @Test
    @DisplayName("toSummaryDTO returns null for a null entity regardless of departmentName")
    void toSummaryDTO_nullSafe() {
        assertNull(DoctorMapper.toSummaryDTO(null, "Cardiology Department"));
    }

    @Test
    @DisplayName("toEntity copies every creation field but leaves the id/timestamps unset")
    void toEntity_copiesCreationFieldsOnly() {
        CreateDoctorDTO dto = new CreateDoctorDTO(
                "dept-2", "James", "Okonkwo", "Neurology", "+15553334444", "james.o@example.com");

        Doctor entity = DoctorMapper.toEntity(dto);

        assertNull(entity.getDoctorId(), "a freshly mapped entity has no id yet — assigned by the caller/DAO");
        assertEquals("dept-2", entity.getDepartmentId());
        assertEquals("James", entity.getFirstName());
        assertEquals("Okonkwo", entity.getLastName());
        assertEquals("Neurology", entity.getSpecialization());
        assertEquals("+15553334444", entity.getPhone());
        assertEquals("james.o@example.com", entity.getEmail());
    }

    @Test
    @DisplayName("toEntity returns null for a null DTO instead of throwing")
    void toEntity_nullSafe() {
        assertNull(DoctorMapper.toEntity(null));
    }

    @Test
    @DisplayName("getSummary() includes the specialization via Doctor's polymorphic override")
    void getSummary_includesSpecialization() {
        Doctor doctor = sample();
        assertEquals("Dr. Sarah Chen | Cardiology", doctor.getSummary());
    }
}
