package hospital.management.backend.service.patient;

import hospital.management.backend.dao.patient.interfaces.VitalSignDAO;
import hospital.management.backend.dto.patient.CreateVitalSignDTO;
import hospital.management.backend.dto.patient.VitalSignDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.patient.VitalSign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
 * Every appointmentId/patientId/vitalId used here is a fresh random UUID rather than a
 * fixed literal — findByAppointment()/findByPatient() read through a real, JVM-wide,
 * static L1 in-process cache with no reset hook exposed to tests, so a fixed id would
 * risk one test's cached DTO leaking into another test's assertions.
 *
 * record()/delete() also call DBConnection.getConnection() internally (via the
 * best-effort resolvePatientId() lookup) to invalidate a by-patient cache entry; that
 * lookup is wrapped in a try/catch that swallows every exception, so it fails silently
 * here (no test DB is configured for this Mockito-only test class) without affecting
 * any assertion below.
 */
@ExtendWith(MockitoExtension.class)
class VitalSignServiceImplTest {

    @Mock
    private VitalSignDAO vitalSignDAO;

    private VitalSignServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VitalSignServiceImpl(vitalSignDAO);
    }

    private VitalSign sampleVitalSign(String vitalId, String appointmentId) {
        VitalSign v = new VitalSign();
        v.setVitalId(vitalId);
        v.setAppointmentId(appointmentId);
        v.setBloodPressureSystolic(120);
        v.setBloodPressureDiastolic(80);
        v.setHeartRate(72);
        v.setTemperatureCelsius(new BigDecimal("36.6"));
        v.setWeightKg(new BigDecimal("70.5"));
        v.setHeightCm(new BigDecimal("175.0"));
        v.setRecordedAt(LocalDateTime.now());
        return v;
    }

    // ── record ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("record throws IllegalArgumentException when appointmentId is blank")
    void record_throwsIllegalArgumentException_whenAppointmentIdBlank() {
        CreateVitalSignDTO dto = new CreateVitalSignDTO("  ", 120, 80, 72, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.record(dto));
        verifyNoInteractions(vitalSignDAO);
    }

    @Test
    @DisplayName("record throws IllegalArgumentException when appointmentId is not a valid UUID")
    void record_throwsIllegalArgumentException_whenAppointmentIdNotValidUuid() {
        CreateVitalSignDTO dto = new CreateVitalSignDTO("not-a-uuid", 120, 80, 72, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.record(dto));
        verifyNoInteractions(vitalSignDAO);
    }

    @Test
    @DisplayName("record throws IllegalStateException when systolic blood pressure is outside 1-300")
    void record_throwsIllegalStateException_whenSystolicOutOfRange() {
        CreateVitalSignDTO dto = new CreateVitalSignDTO(
                UUID.randomUUID().toString(), 301, 80, 72, null, null, null);

        assertThrows(IllegalStateException.class, () -> service.record(dto));
        verifyNoInteractions(vitalSignDAO);
    }

    @Test
    @DisplayName("record throws IllegalStateException when diastolic blood pressure is outside 1-200")
    void record_throwsIllegalStateException_whenDiastolicOutOfRange() {
        CreateVitalSignDTO dto = new CreateVitalSignDTO(
                UUID.randomUUID().toString(), 120, 201, 72, null, null, null);

        assertThrows(IllegalStateException.class, () -> service.record(dto));
        verifyNoInteractions(vitalSignDAO);
    }

    @Test
    @DisplayName("record throws ValidationException when heart rate is non-positive")
    void record_throwsValidationException_whenHeartRateNonPositive() {
        CreateVitalSignDTO dto = new CreateVitalSignDTO(
                UUID.randomUUID().toString(), 120, 80, 0, null, null, null);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.record(dto));
        assertEquals("heartRate", ex.getField());
        verifyNoInteractions(vitalSignDAO);
    }

    @Test
    @DisplayName("record saves the vital sign with the validated fields when everything is valid")
    void record_savesVitalSign_whenValid() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        CreateVitalSignDTO dto = new CreateVitalSignDTO(
                appointmentId, 130, 85, 75, new BigDecimal("37.0"), new BigDecimal("68.0"), new BigDecimal("170.0"));
        when(vitalSignDAO.save(any(VitalSign.class))).thenAnswer(inv -> {
            VitalSign v = inv.getArgument(0);
            v.setVitalId(UUID.randomUUID().toString());
            v.setRecordedAt(LocalDateTime.now());
            return v;
        });

        VitalSignDTO result = service.record(dto);

        ArgumentCaptor<VitalSign> captor = ArgumentCaptor.forClass(VitalSign.class);
        verify(vitalSignDAO).save(captor.capture());
        assertEquals(130, captor.getValue().getBloodPressureSystolic());
        assertEquals(appointmentId, result.getAppointmentId());
        assertEquals(75, result.getHeartRate());
    }

    @Test
    @DisplayName("record allows every measurement to be null — only appointmentId is required")
    void record_allowsAllMeasurementsNull() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        CreateVitalSignDTO dto = new CreateVitalSignDTO(appointmentId, null, null, null, null, null, null);
        when(vitalSignDAO.save(any(VitalSign.class))).thenAnswer(inv -> {
            VitalSign v = inv.getArgument(0);
            v.setVitalId(UUID.randomUUID().toString());
            v.setRecordedAt(LocalDateTime.now());
            return v;
        });

        assertDoesNotThrow(() -> service.record(dto));
    }

    // ── findByAppointment ─────────────────────────────────────────────────

    @Test
    @DisplayName("findByAppointment returns a mapped DTO when the DAO finds a matching vital sign")
    void findByAppointment_returnsMappedDto_whenFound() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        VitalSign vitalSign = sampleVitalSign(UUID.randomUUID().toString(), appointmentId);
        when(vitalSignDAO.findByAppointmentId(appointmentId)).thenReturn(Optional.of(vitalSign));

        VitalSignDTO result = service.findByAppointment(appointmentId);

        assertEquals(appointmentId, result.getAppointmentId());
        assertEquals(72, result.getHeartRate());
        verify(vitalSignDAO).findByAppointmentId(appointmentId);
    }

    @Test
    @DisplayName("findByAppointment throws ResourceNotFoundException when the DAO finds nothing")
    void findByAppointment_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        when(vitalSignDAO.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByAppointment(appointmentId));
    }

    // ── findByPatient ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findByPatient returns mapped DTOs for every vital sign the DAO finds")
    void findByPatient_returnsMappedDtos() throws Exception {
        String patientId = UUID.randomUUID().toString();
        VitalSign vitalSign = sampleVitalSign(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        when(vitalSignDAO.findByPatientId(patientId)).thenReturn(List.of(vitalSign));

        List<VitalSignDTO> result = service.findByPatient(patientId);

        assertEquals(1, result.size());
        verify(vitalSignDAO).findByPatientId(patientId);
    }

    @Test
    @DisplayName("findByPatient returns an empty list when the DAO finds nothing")
    void findByPatient_returnsEmptyList_whenNoneFound() throws Exception {
        String patientId = UUID.randomUUID().toString();
        when(vitalSignDAO.findByPatientId(patientId)).thenReturn(Collections.emptyList());

        List<VitalSignDTO> result = service.findByPatient(patientId);

        assertTrue(result.isEmpty());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete throws ResourceNotFoundException when the vital sign doesn't exist")
    void delete_throwsResourceNotFoundException_whenMissing() throws Exception {
        String vitalId = UUID.randomUUID().toString();
        when(vitalSignDAO.findById(vitalId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(vitalId));
        verify(vitalSignDAO, never()).softDelete(anyString());
    }

    @Test
    @DisplayName("delete soft-deletes the vital sign when it exists")
    void delete_softDeletesExistingVitalSign() throws Exception {
        String vitalId = UUID.randomUUID().toString();
        String appointmentId = UUID.randomUUID().toString();
        when(vitalSignDAO.findById(vitalId)).thenReturn(Optional.of(sampleVitalSign(vitalId, appointmentId)));

        service.delete(vitalId);

        verify(vitalSignDAO).softDelete(vitalId);
    }
}
