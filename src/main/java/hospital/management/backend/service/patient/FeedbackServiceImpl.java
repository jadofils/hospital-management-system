package hospital.management.backend.service.patient;

import com.fasterxml.jackson.core.type.TypeReference;
import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.patient.interfaces.PatientFeedbackDAO;
import hospital.management.backend.dto.patient.CreatePatientFeedbackDTO;
import hospital.management.backend.dto.patient.PatientFeedbackDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.mapper.patient.PatientFeedbackMapper;
import hospital.management.backend.model.patient.PatientFeedback;
import hospital.management.backend.service.patient.interfaces.FeedbackService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.backend.service.log.ServiceAudit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FeedbackServiceImpl implements FeedbackService {

    private final PatientFeedbackDAO feedbackDAO;

    public FeedbackServiceImpl(PatientFeedbackDAO feedbackDAO) {
        this.feedbackDAO = feedbackDAO;
    }

    @Override
    public PatientFeedbackDTO submit(CreatePatientFeedbackDTO dto) throws Exception {
        String patientId = ValidatorUtils.requireNonBlank(dto.getPatientId(), "patientId");
        ValidatorUtils.requireValidUuid(patientId, "patientId");
        if (dto.getAppointmentId() != null && !dto.getAppointmentId().isBlank()) {
            ValidatorUtils.requireValidUuid(dto.getAppointmentId(), "appointmentId");
        }
        if (dto.getRating() == null) {
            throw new IllegalArgumentException("rating must not be null.");
        }
        ValidatorUtils.requireRange(dto.getRating(), 1, 5, "rating");
        String comments = ValidatorUtils.requireNonBlank(dto.getComments(), "comments");
        ValidatorUtils.requireMinLength(comments, 5, "comments");
        ValidatorUtils.requireMaxLength(comments, 1000, "comments");

        // A single INSERT is already atomic — no TransactionManager needed here.
        CacheService.evict(CacheKey.feedback(patientId));
        CacheService.evict(CacheKey.feedbackList());
        PatientFeedback saved = feedbackDAO.save(PatientFeedbackMapper.toEntity(dto));
        // record audit and publish event
        ServiceAudit.record("patient_feedback", "create", saved.getFeedbackId());
        EventBus.publish(AppEventType.PATIENT_FEEDBACK_SUBMITTED, saved.getFeedbackId());
        return PatientFeedbackMapper.toDTO(saved);
    }

    @Override
    public List<PatientFeedbackDTO> findAll() throws Exception {
        Optional<List<PatientFeedbackDTO>> cached = CacheService.get(
            CacheKey.feedbackList(),
            new TypeReference<List<PatientFeedbackDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<PatientFeedbackDTO> dtos = new ArrayList<>();
        for (PatientFeedback feedback : feedbackDAO.findAll()) {
            dtos.add(PatientFeedbackMapper.toDTO(feedback));
        }
        CacheService.set(CacheKey.feedbackList(), dtos, CacheDomain.PATIENT);
        return dtos;
    }

    @Override
    public List<PatientFeedbackDTO> findByPatient(String patientId) throws Exception {
        Optional<List<PatientFeedbackDTO>> cached = CacheService.get(
            CacheKey.feedback(patientId),
            new TypeReference<List<PatientFeedbackDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<PatientFeedbackDTO> dtos = new ArrayList<>();
        for (PatientFeedback feedback : feedbackDAO.findByPatientId(patientId)) {
            dtos.add(PatientFeedbackMapper.toDTO(feedback));
        }
        CacheService.set(CacheKey.feedback(patientId), dtos, CacheDomain.PATIENT);
        return dtos;
    }

    @Override
    public void delete(String feedbackId) throws Exception {
        PatientFeedback feedback = feedbackDAO.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("PatientFeedback", feedbackId));

        // No PATIENT_FEEDBACK_REMOVED event exists in AppEventType — only SUBMITTED
        // is defined for this domain, so deletion is cache-invalidation only.
        CacheService.evict(CacheKey.feedback(feedback.getPatientId()));
        CacheService.evict(CacheKey.feedbackList());
        feedbackDAO.softDelete(feedbackId);
        ServiceAudit.record("patient_feedback", "delete", feedbackId);
    }
}