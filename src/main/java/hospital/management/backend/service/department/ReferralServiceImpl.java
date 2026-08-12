package hospital.management.backend.service.department;

import com.fasterxml.jackson.core.type.TypeReference;
import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.department.interfaces.ReferralDAO;
import hospital.management.backend.dto.doctor.CreateReferralDTO;
import hospital.management.backend.dto.doctor.ReferralDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.doctor.ReferralMapper;
import hospital.management.backend.model.doctor.Referral;
import hospital.management.backend.model.enums.ReferralStatus;
import hospital.management.backend.service.department.interfaces.ReferralService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReferralServiceImpl implements ReferralService {

    private final ReferralDAO referralDAO;

    public ReferralServiceImpl(ReferralDAO referralDAO) {
        this.referralDAO = referralDAO;
    }

    @Override
    public ReferralDTO create(CreateReferralDTO dto) throws Exception {
        ValidatorUtils.requireNonBlank(dto.getAppointmentId(), "appointmentId");
        ValidatorUtils.requireNonBlank(dto.getReferringDoctorId(), "referringDoctorId");
        String referredToDoctorId = ValidatorUtils.requireNonBlank(dto.getReferredToDoctorId(), "referredToDoctorId");

        // Mirrors the `chk_referral_not_self` DB constraint.
        if (referredToDoctorId.equals(dto.getReferringDoctorId())) {
            throw new ValidationException("referredToDoctorId", "A doctor cannot refer a patient to themselves.");
        }

        CacheService.evict(CacheKey.referralsByAppt(dto.getAppointmentId()));
        Referral saved = referralDAO.save(ReferralMapper.toEntity(dto));
        EventBus.publish(AppEventType.REFERRAL_CREATED, saved.getReferralId());
        return ReferralMapper.toDTO(saved);
    }

    @Override
    public ReferralDTO findById(String referralId) throws Exception {
        Optional<ReferralDTO> cached = CacheService.get(CacheKey.referral(referralId), ReferralDTO.class);
        if (cached.isPresent()) return cached.get();

        Referral referral = referralDAO.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral", referralId));
        ReferralDTO dto = ReferralMapper.toDTO(referral);
        CacheService.set(CacheKey.referral(referralId), dto, CacheDomain.DOCTOR);
        return dto;
    }

    @Override
    public List<ReferralDTO> findByAppointment(String appointmentId) throws Exception {
        Optional<List<ReferralDTO>> cached = CacheService.get(
                CacheKey.referralsByAppt(appointmentId),
                new TypeReference<List<ReferralDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<ReferralDTO> dtos = new ArrayList<>();
        for (Referral referral : referralDAO.findByAppointmentId(appointmentId)) {
            dtos.add(ReferralMapper.toDTO(referral));
        }
        CacheService.set(CacheKey.referralsByAppt(appointmentId), dtos, CacheDomain.DOCTOR);
        return dtos;
    }

    @Override
    public List<ReferralDTO> findByReferredToDoctor(String doctorId) throws Exception {
        List<ReferralDTO> dtos = new ArrayList<>();
        for (Referral referral : referralDAO.findByReferredToDoctorId(doctorId)) {
            dtos.add(ReferralMapper.toDTO(referral));
        }
        return dtos;
    }

    @Override
    public ReferralDTO updateStatus(String referralId, String status) throws Exception {
        String rawStatus = ValidatorUtils.requireNonBlank(status, "status");
        ReferralStatus parsed;
        try {
            parsed = ReferralStatus.fromDbValue(rawStatus);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("status", "Invalid referral status: " + rawStatus);
        }

        Referral existing = referralDAO.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral", referralId));

        CacheService.evict(CacheKey.referral(referralId));
        CacheService.evict(CacheKey.referralsByAppt(existing.getAppointmentId()));
        Referral saved = referralDAO.updateStatus(referralId, parsed.getDbValue());
        EventBus.publish(AppEventType.REFERRAL_UPDATED, referralId);
        return ReferralMapper.toDTO(saved);
    }

    @Override
    public void delete(String referralId) throws Exception {
        Referral existing = referralDAO.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral", referralId));

        CacheService.evict(CacheKey.referral(referralId));
        CacheService.evict(CacheKey.referralsByAppt(existing.getAppointmentId()));
        referralDAO.softDelete(referralId);
        // No dedicated REFERRAL_DELETED event exists in AppEventType — reusing
        // REFERRAL_UPDATED for the delete notification, same as DOCTOR_SCHEDULE_UPDATED
        // is reused for every schedule mutation (create/update/delete) below.
        EventBus.publish(AppEventType.REFERRAL_UPDATED, referralId);
    }
}
