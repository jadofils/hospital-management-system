package hospital.management.backend.service.department;

import hospital.management.backend.dao.department.interfaces.ReferralDAO;
import hospital.management.backend.dto.doctor.CreateReferralDTO;
import hospital.management.backend.dto.doctor.ReferralDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.doctor.Referral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Every id used here is a fresh random UUID rather than a fixed literal —
 * findById()/findByAppointment() read through a real, JVM-wide, static L1
 * in-process cache with no reset hook exposed to tests, so a fixed id would
 * risk one test's cached DTO leaking into another test's assertions (see
 * DoctorServiceImplTest for the same rationale).
 */
@ExtendWith(MockitoExtension.class)
class ReferralServiceImplTest {

    @Mock
    private ReferralDAO referralDAO;

    private ReferralServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReferralServiceImpl(referralDAO);
    }

    private Referral sampleReferral(String id, String appointmentId, String referringDoctorId, String referredToDoctorId) {
        Referral r = new Referral();
        r.setReferralId(id);
        r.setAppointmentId(appointmentId);
        r.setReferringDoctorId(referringDoctorId);
        r.setReferredToDoctorId(referredToDoctorId);
        r.setReason("Needs specialist opinion");
        r.setStatus("pending");
        return r;
    }

    private CreateReferralDTO sampleCreateDto(String appointmentId, String referringDoctorId, String referredToDoctorId) {
        return new CreateReferralDTO(appointmentId, referringDoctorId, referredToDoctorId, "Needs specialist opinion");
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create throws IllegalArgumentException when appointmentId is blank")
    void create_throwsIllegalArgumentException_whenAppointmentIdBlank() {
        CreateReferralDTO dto = new CreateReferralDTO(
                "  ", UUID.randomUUID().toString(), UUID.randomUUID().toString(), "reason");

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        verifyNoInteractions(referralDAO);
    }

    @Test
    @DisplayName("create throws IllegalArgumentException when referringDoctorId is blank")
    void create_throwsIllegalArgumentException_whenReferringDoctorIdBlank() {
        CreateReferralDTO dto = new CreateReferralDTO(
                UUID.randomUUID().toString(), "  ", UUID.randomUUID().toString(), "reason");

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
    }

    @Test
    @DisplayName("create throws IllegalArgumentException when referredToDoctorId is blank")
    void create_throwsIllegalArgumentException_whenReferredToDoctorIdBlank() {
        CreateReferralDTO dto = new CreateReferralDTO(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), "  ", "reason");

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
    }

    @Test
    @DisplayName("create throws ValidationException when referring and referred-to doctors are the same — mirrors chk_referral_not_self")
    void create_throwsValidationException_whenSelfReferral() {
        String doctorId = UUID.randomUUID().toString();
        CreateReferralDTO dto = sampleCreateDto(UUID.randomUUID().toString(), doctorId, doctorId);

        assertThrows(ValidationException.class, () -> service.create(dto));
        verifyNoInteractions(referralDAO);
    }

    @Test
    @DisplayName("create saves a new referral defaulting status to pending when everything is valid")
    void create_savesReferral_whenValid() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        String referringDoctorId = UUID.randomUUID().toString();
        String referredToDoctorId = UUID.randomUUID().toString();
        CreateReferralDTO dto = sampleCreateDto(appointmentId, referringDoctorId, referredToDoctorId);
        when(referralDAO.save(any(Referral.class))).thenAnswer(inv -> inv.getArgument(0));

        ReferralDTO result = service.create(dto);

        ArgumentCaptor<Referral> captor = ArgumentCaptor.forClass(Referral.class);
        verify(referralDAO).save(captor.capture());
        assertEquals("pending", captor.getValue().getStatus());
        assertEquals(referringDoctorId, result.getReferringDoctorId());
        assertEquals(referredToDoctorId, result.getReferredToDoctorId());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns a mapped DTO when the DAO finds a matching referral")
    void findById_returnsMappedDto_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        Referral referral = sampleReferral(id, UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString());
        when(referralDAO.findById(id)).thenReturn(Optional.of(referral));

        ReferralDTO dto = service.findById(id);

        assertEquals(id, dto.getReferralId());
        assertEquals("pending", dto.getStatus());
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the DAO finds nothing")
    void findById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(referralDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    // ── findByAppointment ─────────────────────────────────────────────────

    @Test
    @DisplayName("findByAppointment maps every DAO referral to a DTO")
    void findByAppointment_mapsEveryReferral() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        Referral r1 = sampleReferral(UUID.randomUUID().toString(), appointmentId,
                UUID.randomUUID().toString(), UUID.randomUUID().toString());
        Referral r2 = sampleReferral(UUID.randomUUID().toString(), appointmentId,
                UUID.randomUUID().toString(), UUID.randomUUID().toString());
        when(referralDAO.findByAppointmentId(appointmentId)).thenReturn(List.of(r1, r2));

        List<ReferralDTO> result = service.findByAppointment(appointmentId);

        assertEquals(2, result.size());
    }

    // ── updateStatus ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus throws ValidationException for a status value outside the enum")
    void updateStatus_throwsValidationException_whenStatusInvalid() throws Exception {
        String id = UUID.randomUUID().toString();

        assertThrows(ValidationException.class, () -> service.updateStatus(id, "bogus-status"));
        verifyNoInteractions(referralDAO);
    }

    @Test
    @DisplayName("updateStatus throws ResourceNotFoundException when the referral doesn't exist")
    void updateStatus_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(referralDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateStatus(id, "scheduled"));
        verify(referralDAO, never()).updateStatus(anyString(), anyString());
    }

    @Test
    @DisplayName("updateStatus normalizes the status to its lowercase DB value, case-insensitively")
    void updateStatus_updatesStatus_whenValid() throws Exception {
        String id = UUID.randomUUID().toString();
        Referral existing = sampleReferral(id, UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString());
        when(referralDAO.findById(id)).thenReturn(Optional.of(existing));
        Referral updated = sampleReferral(id, existing.getAppointmentId(),
                existing.getReferringDoctorId(), existing.getReferredToDoctorId());
        updated.setStatus("scheduled");
        when(referralDAO.updateStatus(id, "scheduled")).thenReturn(updated);

        ReferralDTO result = service.updateStatus(id, "SCHEDULED");

        assertEquals("scheduled", result.getStatus());
        verify(referralDAO).updateStatus(id, "scheduled");
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete throws ResourceNotFoundException when the referral doesn't exist")
    void delete_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(referralDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
        verify(referralDAO, never()).softDelete(anyString());
    }

    @Test
    @DisplayName("delete soft-deletes an existing referral")
    void delete_softDeletes_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        Referral existing = sampleReferral(id, UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString());
        when(referralDAO.findById(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        verify(referralDAO).softDelete(id);
    }
}
