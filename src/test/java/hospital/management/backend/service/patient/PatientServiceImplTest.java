package hospital.management.backend.service.patient;

import hospital.management.backend.dao.patient.interfaces.PatientDAO;
import hospital.management.backend.dto.patient.CreatePatientDTO;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.patient.UpdatePatientDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.patient.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Every id used here is a fresh random UUID rather than a fixed literal —
 * findById()/CacheService.get() reads through a real, JVM-wide, static L1
 * in-process cache with no reset hook exposed to tests, so a fixed id would
 * risk one test's cached DTO leaking into another test's assertions.
 */
@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientDAO patientDAO;

    private PatientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PatientServiceImpl(patientDAO);
    }

    private Patient samplePatient(String id) {
        Patient p = new Patient();
        p.setPatientId(id);
        p.setFirstName("Jane");
        p.setLastName("Doe");
        p.setDob(LocalDate.of(1990, 5, 20));
        p.setGender("Female");
        p.setEmail("jane.doe@example.com");
        return p;
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns a mapped DTO when the DAO finds a matching, non-deleted patient")
    void findById_returnsMappedDto_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(patientDAO.findById(id)).thenReturn(Optional.of(samplePatient(id)));

        PatientDTO dto = service.findById(id);

        assertEquals(id, dto.getPatientId());
        assertEquals("Jane", dto.getFirstName());
        verify(patientDAO).findById(id);
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the DAO finds nothing")
    void findById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(patientDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create throws IllegalArgumentException when firstName is blank")
    void create_throwsIllegalArgumentException_whenFirstNameBlank() {
        CreatePatientDTO dto = new CreatePatientDTO("  ", "Doe", LocalDate.of(1990, 1, 1),
                "Female", null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        verifyNoInteractions(patientDAO);
    }

    @Test
    @DisplayName("create throws ValidationException when date of birth is missing")
    void create_throwsValidationException_whenDobMissing() {
        CreatePatientDTO dto = new CreatePatientDTO("Jane", "Doe", null,
                "Female", null, null, null);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.create(dto));
        assertTrue(ex.getMessage().toLowerCase().contains("birth"));
    }

    @Test
    @DisplayName("create throws IllegalArgumentException when the email is malformed")
    void create_throwsIllegalArgumentException_whenEmailMalformed() {
        CreatePatientDTO dto = new CreatePatientDTO("Jane", "Doe", LocalDate.of(1990, 1, 1),
                "Female", null, "not-an-email", null);

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
    }

    @Test
    @DisplayName("create throws ValidationException when the email is already registered to another patient")
    void create_throwsValidationException_whenEmailAlreadyRegistered() throws Exception {
        CreatePatientDTO dto = new CreatePatientDTO("Jane", "Doe", LocalDate.of(1990, 1, 1),
                "Female", null, "jane.doe@example.com", null);
        when(patientDAO.findByEmail("jane.doe@example.com"))
                .thenReturn(Optional.of(samplePatient(UUID.randomUUID().toString())));

        assertThrows(ValidationException.class, () -> service.create(dto));
        verify(patientDAO, never()).save(any());
    }

    @Test
    @DisplayName("create saves a new patient with the validated fields when everything is valid")
    void create_savesPatient_whenValid() throws Exception {
        CreatePatientDTO dto = new CreatePatientDTO("Jane", "Doe", LocalDate.of(1990, 1, 1),
                "Female", "+15558675309", "jane.doe@example.com", "123 Main St");
        when(patientDAO.findByEmail("jane.doe@example.com")).thenReturn(Optional.empty());
        when(patientDAO.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientDTO result = service.create(dto);

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientDAO).save(captor.capture());
        Patient saved = captor.getValue();
        assertEquals("Jane", saved.getFirstName());
        assertEquals("Doe", saved.getLastName());
        assertEquals("jane.doe@example.com", result.getEmail());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update throws ResourceNotFoundException when the patient doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(patientDAO.findById(id)).thenReturn(Optional.empty());
        UpdatePatientDTO dto = new UpdatePatientDTO(id, "+15551234567", null, null);

        assertThrows(ResourceNotFoundException.class, () -> service.update(dto));
    }

    @Test
    @DisplayName("update throws ValidationException when changing to an email already used by someone else")
    void update_throwsValidationException_whenEmailTakenBySomeoneElse() throws Exception {
        String id = UUID.randomUUID().toString();
        Patient existing = samplePatient(id);
        when(patientDAO.findById(id)).thenReturn(Optional.of(existing));
        when(patientDAO.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(samplePatient(UUID.randomUUID().toString())));

        UpdatePatientDTO dto = new UpdatePatientDTO(id, null, "taken@example.com", null);

        assertThrows(ValidationException.class, () -> service.update(dto));
        verify(patientDAO, never()).update(any());
    }

    @Test
    @DisplayName("update allows keeping the patient's own current email unchanged without a conflict check")
    void update_allowsKeepingOwnEmail() throws Exception {
        String id = UUID.randomUUID().toString();
        Patient existing = samplePatient(id); // email = jane.doe@example.com
        when(patientDAO.findById(id)).thenReturn(Optional.of(existing));
        when(patientDAO.update(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePatientDTO dto = new UpdatePatientDTO(id, "+15559998888", "jane.doe@example.com", "New Address");

        PatientDTO result = service.update(dto);

        assertEquals("New Address", result.getAddress());
        assertEquals("+15559998888", result.getPhone());
        verify(patientDAO, never()).findByEmail(anyString());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete delegates to the DAO's soft-delete for the given id")
    void delete_delegatesToSoftDelete() throws Exception {
        String id = UUID.randomUUID().toString();

        service.delete(id);

        verify(patientDAO).softDelete(id);
    }
}
