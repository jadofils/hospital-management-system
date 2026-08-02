package hospital.management.backend.service.patient;

import hospital.management.backend.dao.patient.interfaces.PatientAllergyDAO;
import hospital.management.backend.dto.patient.CreatePatientAllergyDTO;
import hospital.management.backend.dto.patient.PatientAllergyDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.patient.PatientAllergy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Every patientId/allergyId used here is a fresh random UUID rather than a fixed literal —
 * findByPatient()/CacheService.get() reads through a real, JVM-wide, static L1 in-process
 * cache with no reset hook exposed to tests, so a fixed id would risk one test's cached
 * DTO list leaking into another test's assertions.
 */
@ExtendWith(MockitoExtension.class)
class AllergyServiceImplTest {

    @Mock
    private PatientAllergyDAO allergyDAO;

    private AllergyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AllergyServiceImpl(allergyDAO);
    }

    private PatientAllergy sampleAllergy(String allergyId, String patientId) {
        PatientAllergy a = new PatientAllergy();
        a.setAllergyId(allergyId);
        a.setPatientId(patientId);
        a.setAllergen("Penicillin");
        a.setReaction("Hives");
        a.setSeverity("moderate");
        a.setCreatedAt(LocalDateTime.now());
        return a;
    }

    // ── add ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("add throws IllegalArgumentException when patientId is blank")
    void add_throwsIllegalArgumentException_whenPatientIdBlank() {
        CreatePatientAllergyDTO dto = new CreatePatientAllergyDTO("  ", "Penicillin", "Hives", "mild");

        assertThrows(IllegalArgumentException.class, () -> service.add(dto));
        verifyNoInteractions(allergyDAO);
    }

    @Test
    @DisplayName("add throws IllegalArgumentException when patientId is not a valid UUID")
    void add_throwsIllegalArgumentException_whenPatientIdNotValidUuid() {
        CreatePatientAllergyDTO dto = new CreatePatientAllergyDTO("not-a-uuid", "Penicillin", "Hives", "mild");

        assertThrows(IllegalArgumentException.class, () -> service.add(dto));
        verifyNoInteractions(allergyDAO);
    }

    @Test
    @DisplayName("add throws IllegalArgumentException when allergen is blank")
    void add_throwsIllegalArgumentException_whenAllergenBlank() {
        CreatePatientAllergyDTO dto = new CreatePatientAllergyDTO(UUID.randomUUID().toString(), "  ", "Hives", "mild");

        assertThrows(IllegalArgumentException.class, () -> service.add(dto));
        verifyNoInteractions(allergyDAO);
    }

    @Test
    @DisplayName("add throws IllegalArgumentException when severity is not one of mild/moderate/severe")
    void add_throwsIllegalArgumentException_whenSeverityInvalid() {
        CreatePatientAllergyDTO dto = new CreatePatientAllergyDTO(
                UUID.randomUUID().toString(), "Penicillin", "Hives", "critical");

        assertThrows(IllegalArgumentException.class, () -> service.add(dto));
        verifyNoInteractions(allergyDAO);
    }

    @Test
    @DisplayName("add saves an allergy with a valid mild/moderate/severe severity")
    void add_savesAllergy_whenSeverityValid() throws Exception {
        String patientId = UUID.randomUUID().toString();
        CreatePatientAllergyDTO dto = new CreatePatientAllergyDTO(patientId, "Penicillin", "Hives", "severe");
        when(allergyDAO.save(any(PatientAllergy.class))).thenAnswer(inv -> {
            PatientAllergy a = inv.getArgument(0);
            a.setAllergyId(UUID.randomUUID().toString());
            a.setCreatedAt(LocalDateTime.now());
            return a;
        });

        PatientAllergyDTO result = service.add(dto);

        ArgumentCaptor<PatientAllergy> captor = ArgumentCaptor.forClass(PatientAllergy.class);
        verify(allergyDAO).save(captor.capture());
        assertEquals("Penicillin", captor.getValue().getAllergen());
        assertEquals("severe", captor.getValue().getSeverity());
        assertEquals(patientId, result.getPatientId());
    }

    @Test
    @DisplayName("add allows a null severity without validating it against the enum")
    void add_allowsNullSeverity() throws Exception {
        String patientId = UUID.randomUUID().toString();
        CreatePatientAllergyDTO dto = new CreatePatientAllergyDTO(patientId, "Penicillin", "Hives", null);
        when(allergyDAO.save(any(PatientAllergy.class))).thenAnswer(inv -> {
            PatientAllergy a = inv.getArgument(0);
            a.setAllergyId(UUID.randomUUID().toString());
            a.setCreatedAt(LocalDateTime.now());
            return a;
        });

        assertDoesNotThrow(() -> service.add(dto));
        verify(allergyDAO).save(any(PatientAllergy.class));
    }

    // ── findByPatient ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findByPatient returns mapped DTOs for every allergy the DAO finds")
    void findByPatient_returnsMappedDtos() throws Exception {
        String patientId = UUID.randomUUID().toString();
        PatientAllergy allergy = sampleAllergy(UUID.randomUUID().toString(), patientId);
        when(allergyDAO.findByPatientId(patientId)).thenReturn(List.of(allergy));

        List<PatientAllergyDTO> result = service.findByPatient(patientId);

        assertEquals(1, result.size());
        assertEquals("Penicillin", result.get(0).getAllergen());
        verify(allergyDAO).findByPatientId(patientId);
    }

    @Test
    @DisplayName("findByPatient returns an empty list when the DAO finds nothing")
    void findByPatient_returnsEmptyList_whenNoneFound() throws Exception {
        String patientId = UUID.randomUUID().toString();
        when(allergyDAO.findByPatientId(patientId)).thenReturn(Collections.emptyList());

        List<PatientAllergyDTO> result = service.findByPatient(patientId);

        assertTrue(result.isEmpty());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete throws ResourceNotFoundException when the allergy doesn't exist")
    void delete_throwsResourceNotFoundException_whenMissing() throws Exception {
        String allergyId = UUID.randomUUID().toString();
        when(allergyDAO.findById(allergyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(allergyId));
        verify(allergyDAO, never()).softDelete(anyString());
    }

    @Test
    @DisplayName("delete soft-deletes the allergy when it exists")
    void delete_softDeletesExistingAllergy() throws Exception {
        String allergyId = UUID.randomUUID().toString();
        String patientId = UUID.randomUUID().toString();
        when(allergyDAO.findById(allergyId)).thenReturn(Optional.of(sampleAllergy(allergyId, patientId)));

        service.delete(allergyId);

        verify(allergyDAO).softDelete(allergyId);
    }
}
