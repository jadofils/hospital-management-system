package hospital.management.backend.service.patient;

import hospital.management.backend.dao.patient.interfaces.PatientFeedbackDAO;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dto.patient.CreatePatientFeedbackDTO;
import hospital.management.backend.dto.patient.PatientFeedbackDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.patient.PatientFeedback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
 * Every patientId/feedbackId used here is a fresh random UUID rather than a fixed literal —
 * findByPatient()/CacheService.get() reads through a real, JVM-wide, static L1 in-process
 * cache with no reset hook exposed to tests, so a fixed id would risk one test's cached
 * DTO list leaking into another test's assertions.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @Mock
    private PatientFeedbackDAO feedbackDAO;

    private FeedbackServiceImpl service;

    @BeforeEach
    void setUp() {
        CacheService.evict(CacheKey.feedbackList());
        service = new FeedbackServiceImpl(feedbackDAO);
    }

    private PatientFeedback sampleFeedback(String feedbackId, String patientId) {
        PatientFeedback f = new PatientFeedback();
        f.setFeedbackId(feedbackId);
        f.setPatientId(patientId);
        f.setRating(5);
        f.setComments("Great service");
        f.setDateSubmitted(LocalDate.now());
        f.setCreatedAt(LocalDateTime.now());
        return f;
    }

    // ── submit ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("submit throws IllegalArgumentException when patientId is blank")
    void submit_throwsIllegalArgumentException_whenPatientIdBlank() {
        CreatePatientFeedbackDTO dto = new CreatePatientFeedbackDTO("  ", null, 5, "Great", null);

        assertThrows(IllegalArgumentException.class, () -> service.submit(dto));
        verifyNoInteractions(feedbackDAO);
    }

    @Test
    @DisplayName("submit throws IllegalArgumentException when patientId is not a valid UUID")
    void submit_throwsIllegalArgumentException_whenPatientIdNotValidUuid() {
        CreatePatientFeedbackDTO dto = new CreatePatientFeedbackDTO("not-a-uuid", null, 5, "Great", null);

        assertThrows(IllegalArgumentException.class, () -> service.submit(dto));
        verifyNoInteractions(feedbackDAO);
    }

    @Test
    @DisplayName("submit throws IllegalArgumentException when appointmentId is supplied but not a valid UUID")
    void submit_throwsIllegalArgumentException_whenAppointmentIdNotValidUuid() {
        CreatePatientFeedbackDTO dto = new CreatePatientFeedbackDTO(
                UUID.randomUUID().toString(), "not-a-uuid", 5, "Great", null);

        assertThrows(IllegalArgumentException.class, () -> service.submit(dto));
        verifyNoInteractions(feedbackDAO);
    }

    @Test
    @DisplayName("submit throws IllegalArgumentException when rating is null")
    void submit_throwsIllegalArgumentException_whenRatingNull() {
        CreatePatientFeedbackDTO dto = new CreatePatientFeedbackDTO(
                UUID.randomUUID().toString(), null, null, "Great", null);

        assertThrows(IllegalArgumentException.class, () -> service.submit(dto));
        verifyNoInteractions(feedbackDAO);
    }

    @Test
    @DisplayName("submit throws IllegalStateException when rating is outside 1-5")
    void submit_throwsIllegalStateException_whenRatingOutOfRange() {
        CreatePatientFeedbackDTO tooHigh = new CreatePatientFeedbackDTO(
                UUID.randomUUID().toString(), null, 6, "Great", null);
        CreatePatientFeedbackDTO tooLow = new CreatePatientFeedbackDTO(
                UUID.randomUUID().toString(), null, 0, "Bad", null);

        assertThrows(IllegalStateException.class, () -> service.submit(tooHigh));
        assertThrows(IllegalStateException.class, () -> service.submit(tooLow));
        verifyNoInteractions(feedbackDAO);
    }

    @Test
    @DisplayName("submit throws IllegalArgumentException when comments is blank")
    void submit_throwsIllegalArgumentException_whenCommentsBlank() {
        CreatePatientFeedbackDTO dto = new CreatePatientFeedbackDTO(
                UUID.randomUUID().toString(), null, 5, "   ", null);

        assertThrows(IllegalArgumentException.class, () -> service.submit(dto));
        verifyNoInteractions(feedbackDAO);
    }

    @Test
    @DisplayName("submit throws IllegalArgumentException when comments is shorter than the minimum length")
    void submit_throwsIllegalArgumentException_whenCommentsTooShort() {
        CreatePatientFeedbackDTO dto = new CreatePatientFeedbackDTO(
                UUID.randomUUID().toString(), null, 5, "Ok", null);

        assertThrows(IllegalArgumentException.class, () -> service.submit(dto));
        verifyNoInteractions(feedbackDAO);
    }

    @Test
    @DisplayName("submit saves feedback with the validated fields when everything is valid")
    void submit_savesFeedback_whenValid() throws Exception {
        String patientId = UUID.randomUUID().toString();
        CreatePatientFeedbackDTO dto = new CreatePatientFeedbackDTO(patientId, null, 4, "Good visit", null);
        when(feedbackDAO.save(any(PatientFeedback.class))).thenAnswer(inv -> {
            PatientFeedback f = inv.getArgument(0);
            f.setFeedbackId(UUID.randomUUID().toString());
            f.setDateSubmitted(LocalDate.now());
            f.setCreatedAt(LocalDateTime.now());
            return f;
        });

        PatientFeedbackDTO result = service.submit(dto);

        ArgumentCaptor<PatientFeedback> captor = ArgumentCaptor.forClass(PatientFeedback.class);
        verify(feedbackDAO).save(captor.capture());
        assertEquals(4, captor.getValue().getRating());
        assertEquals("Good visit", captor.getValue().getComments());
        assertEquals(patientId, result.getPatientId());
    }

    @Test
    @DisplayName("submit allows a null/blank appointmentId without validating it as a UUID")
    void submit_allowsBlankAppointmentId() throws Exception {
        String patientId = UUID.randomUUID().toString();
        CreatePatientFeedbackDTO dto = new CreatePatientFeedbackDTO(patientId, "  ", 5, "Great", null);
        when(feedbackDAO.save(any(PatientFeedback.class))).thenAnswer(inv -> {
            PatientFeedback f = inv.getArgument(0);
            f.setFeedbackId(UUID.randomUUID().toString());
            f.setDateSubmitted(LocalDate.now());
            f.setCreatedAt(LocalDateTime.now());
            return f;
        });

        assertDoesNotThrow(() -> service.submit(dto));
    }

    // ── findByPatient ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findByPatient returns mapped DTOs for every feedback entry the DAO finds")
    void findByPatient_returnsMappedDtos() throws Exception {
        String patientId = UUID.randomUUID().toString();
        PatientFeedback feedback = sampleFeedback(UUID.randomUUID().toString(), patientId);
        when(feedbackDAO.findByPatientId(patientId)).thenReturn(List.of(feedback));

        List<PatientFeedbackDTO> result = service.findByPatient(patientId);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getRating());
        verify(feedbackDAO).findByPatientId(patientId);
    }

    @Test
    @DisplayName("findByPatient returns an empty list when the DAO finds nothing")
    void findByPatient_returnsEmptyList_whenNoneFound() throws Exception {
        String patientId = UUID.randomUUID().toString();
        when(feedbackDAO.findByPatientId(patientId)).thenReturn(Collections.emptyList());

        List<PatientFeedbackDTO> result = service.findByPatient(patientId);

        assertTrue(result.isEmpty());
    }

    // ── findAll ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll returns all mapped DTOs and caches the global feedback list")
    void findAll_returnsMappedDtos_andCachesResult() throws Exception {
        String patientId = UUID.randomUUID().toString();
        PatientFeedback feedback = sampleFeedback(UUID.randomUUID().toString(), patientId);
        when(feedbackDAO.findAll()).thenReturn(List.of(feedback));

        List<PatientFeedbackDTO> first = service.findAll();
        List<PatientFeedbackDTO> second = service.findAll();

        assertEquals(1, first.size());
        assertEquals(1, second.size());
        verify(feedbackDAO, times(1)).findAll();
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete throws ResourceNotFoundException when the feedback doesn't exist")
    void delete_throwsResourceNotFoundException_whenMissing() throws Exception {
        String feedbackId = UUID.randomUUID().toString();
        when(feedbackDAO.findById(feedbackId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(feedbackId));
        verify(feedbackDAO, never()).softDelete(anyString());
    }

    @Test
    @DisplayName("delete soft-deletes the feedback when it exists")
    void delete_softDeletesExistingFeedback() throws Exception {
        String feedbackId = UUID.randomUUID().toString();
        String patientId = UUID.randomUUID().toString();
        when(feedbackDAO.findById(feedbackId)).thenReturn(Optional.of(sampleFeedback(feedbackId, patientId)));

        service.delete(feedbackId);

        verify(feedbackDAO).softDelete(feedbackId);
    }
}
