package hospital.management.backend.service.clinical;

import hospital.management.backend.dao.clinical.interfaces.MedicalRecordDAO;
import hospital.management.backend.dto.clinical.CreateMedicalRecordDTO;
import hospital.management.backend.dto.clinical.MedicalRecordDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.patient.MedicalRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Every id used here is a fresh random UUID rather than a fixed literal — findById()/
 * findByAppointment() each read through a real, JVM-wide, static L1 in-process cache
 * with no reset hook exposed to tests, so a fixed id would risk one test's cached DTO
 * leaking into another test's assertions. None of create()/update()/delete() wraps its
 * DAO call in TransactionManager (each is a single atomic INSERT/UPDATE), so — unlike
 * AuthServiceImplTest — no MockedStatic is needed here.
 */
@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceImplTest {

    @Mock
    private MedicalRecordDAO recordDAO;

    private MedicalRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MedicalRecordServiceImpl(recordDAO);
    }

    private MedicalRecord sampleRecord(String id, String appointmentId) {
        MedicalRecord r = new MedicalRecord();
        r.setRecordId(id);
        r.setAppointmentId(appointmentId);
        r.setDiagnosis("Hypertension");
        r.setSymptoms("Headache, dizziness");
        r.setNotes("Prescribed lisinopril 10mg");
        return r;
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create throws IllegalArgumentException when appointmentId is blank")
    void create_throwsIllegalArgumentException_whenAppointmentIdBlank() {
        CreateMedicalRecordDTO dto = new CreateMedicalRecordDTO("  ", "Hypertension", "Headache", "Notes");

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        verifyNoInteractions(recordDAO);
    }

    @Test
    @DisplayName("create throws ValidationException when a record already exists for that appointment")
    void create_throwsValidationException_whenAppointmentAlreadyHasRecord() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        CreateMedicalRecordDTO dto = new CreateMedicalRecordDTO(appointmentId, "Hypertension", "Headache", "Notes");
        when(recordDAO.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(sampleRecord(UUID.randomUUID().toString(), appointmentId)));

        assertThrows(ValidationException.class, () -> service.create(dto));
        verify(recordDAO, never()).save(any());
    }

    @Test
    @DisplayName("create saves a new record with the validated fields when everything is valid")
    void create_savesRecord_whenValid() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        CreateMedicalRecordDTO dto = new CreateMedicalRecordDTO(appointmentId, "Hypertension", "Headache", "Notes");
        when(recordDAO.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(recordDAO.save(any(MedicalRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecordDTO result = service.create(dto);

        ArgumentCaptor<MedicalRecord> captor = ArgumentCaptor.forClass(MedicalRecord.class);
        verify(recordDAO).save(captor.capture());
        assertEquals(appointmentId, captor.getValue().getAppointmentId());
        assertEquals("Hypertension", result.getDiagnosis());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns a mapped DTO when the DAO finds a matching record")
    void findById_returnsMappedDto_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(recordDAO.findById(id)).thenReturn(Optional.of(sampleRecord(id, UUID.randomUUID().toString())));

        MedicalRecordDTO dto = service.findById(id);

        assertEquals(id, dto.getRecordId());
        assertEquals("Hypertension", dto.getDiagnosis());
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the DAO finds nothing")
    void findById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(recordDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    // ── findByAppointment ─────────────────────────────────────────────────

    @Test
    @DisplayName("findByAppointment returns a mapped DTO when a record exists for that appointment")
    void findByAppointment_returnsMappedDto_whenFound() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        String recordId = UUID.randomUUID().toString();
        when(recordDAO.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(sampleRecord(recordId, appointmentId)));

        MedicalRecordDTO dto = service.findByAppointment(appointmentId);

        assertEquals(recordId, dto.getRecordId());
    }

    @Test
    @DisplayName("findByAppointment throws ResourceNotFoundException when no record exists for that appointment")
    void findByAppointment_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        when(recordDAO.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByAppointment(appointmentId));
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update throws ResourceNotFoundException when the record doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(recordDAO.findById(id)).thenReturn(Optional.empty());
        CreateMedicalRecordDTO dto = new CreateMedicalRecordDTO(null, "Flu", null, null);

        assertThrows(ResourceNotFoundException.class, () -> service.update(id, dto));
    }

    @Test
    @DisplayName("update applies only the fields provided, leaving the rest unchanged")
    void update_appliesOnlyProvidedFields() throws Exception {
        String id = UUID.randomUUID().toString();
        String appointmentId = UUID.randomUUID().toString();
        MedicalRecord existing = sampleRecord(id, appointmentId);
        when(recordDAO.findById(id)).thenReturn(Optional.of(existing));
        when(recordDAO.update(any(MedicalRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateMedicalRecordDTO dto = new CreateMedicalRecordDTO(null, "Migraine", null, null);

        MedicalRecordDTO result = service.update(id, dto);

        assertEquals("Migraine", result.getDiagnosis());
        assertEquals("Headache, dizziness", result.getSymptoms()); // unchanged
        assertEquals("Prescribed lisinopril 10mg", result.getNotes()); // unchanged
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete throws ResourceNotFoundException when the record doesn't exist")
    void delete_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(recordDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
        verify(recordDAO, never()).softDelete(anyString());
    }

    @Test
    @DisplayName("delete delegates to the DAO's soft-delete when the record exists")
    void delete_delegatesToSoftDelete() throws Exception {
        String id = UUID.randomUUID().toString();
        when(recordDAO.findById(id)).thenReturn(Optional.of(sampleRecord(id, UUID.randomUUID().toString())));

        service.delete(id);

        verify(recordDAO).softDelete(id);
    }
}
