package hospital.management.backend.service.patient;

import com.fasterxml.jackson.core.type.TypeReference;
import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.patient.interfaces.VitalSignDAO;
import hospital.management.backend.dto.patient.CreateVitalSignDTO;
import hospital.management.backend.dto.patient.VitalSignDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.patient.VitalSignMapper;
import hospital.management.backend.model.patient.VitalSign;
import hospital.management.backend.service.patient.interfaces.VitalSignService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VitalSignServiceImpl implements VitalSignService {

    private final VitalSignDAO vitalSignDAO;

    public VitalSignServiceImpl(VitalSignDAO vitalSignDAO) {
        this.vitalSignDAO = vitalSignDAO;
    }

    @Override
    public VitalSignDTO record(CreateVitalSignDTO dto) throws Exception {
        String appointmentId = ValidatorUtils.requireNonBlank(dto.getAppointmentId(), "appointmentId");
        ValidatorUtils.requireValidUuid(appointmentId, "appointmentId");

        if (dto.getBloodPressureSystolic() != null) {
            ValidatorUtils.requireRange(dto.getBloodPressureSystolic(), 1, 300, "bloodPressureSystolic");
        }
        if (dto.getBloodPressureDiastolic() != null) {
            ValidatorUtils.requireRange(dto.getBloodPressureDiastolic(), 1, 200, "bloodPressureDiastolic");
        }
        if (dto.getHeartRate() != null && dto.getHeartRate() <= 0) {
            throw new ValidationException("heartRate", "heartRate must be greater than 0.");
        }

        VitalSign vitalSign = VitalSignMapper.toEntity(dto);

        // Delete-before-write on the appointment-scoped cache entry.
        CacheService.evict(CacheKey.vitals(appointmentId));
        VitalSign saved = vitalSignDAO.save(vitalSign);

        // vital_signs carries no patientId of its own (see model javadoc); resolve it
        // best-effort via appointments purely to invalidate the by-patient list cache.
        String patientId = resolvePatientId(appointmentId);
        if (patientId != null) {
            CacheService.evict(CacheKey.vitalsByPatient(patientId));
        }

        EventBus.publish(AppEventType.VITAL_SIGN_RECORDED, saved.getVitalId());
        return VitalSignMapper.toDTO(saved);
    }

    @Override
    public VitalSignDTO findByAppointment(String appointmentId) throws Exception {
        Optional<VitalSignDTO> cached = CacheService.get(CacheKey.vitals(appointmentId), VitalSignDTO.class);
        if (cached.isPresent()) return cached.get();

        VitalSign vitalSign = vitalSignDAO.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("VitalSign", appointmentId));
        VitalSignDTO dto = VitalSignMapper.toDTO(vitalSign);
        CacheService.set(CacheKey.vitals(appointmentId), dto, CacheDomain.PATIENT);
        return dto;
    }

    @Override
    public List<VitalSignDTO> findByPatient(String patientId) throws Exception {
        Optional<List<VitalSignDTO>> cached = CacheService.get(
            CacheKey.vitalsByPatient(patientId),
            new TypeReference<List<VitalSignDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<VitalSignDTO> dtos = new ArrayList<>();
        for (VitalSign vitalSign : vitalSignDAO.findByPatientId(patientId)) {
            dtos.add(VitalSignMapper.toDTO(vitalSign));
        }
        CacheService.set(CacheKey.vitalsByPatient(patientId), dtos, CacheDomain.PATIENT);
        return dtos;
    }

    @Override
    public void delete(String vitalId) throws Exception {
        VitalSign existing = vitalSignDAO.findById(vitalId)
                .orElseThrow(() -> new ResourceNotFoundException("VitalSign", vitalId));

        CacheService.evict(CacheKey.vitals(existing.getAppointmentId()));
        String patientId = resolvePatientId(existing.getAppointmentId());
        if (patientId != null) {
            CacheService.evict(CacheKey.vitalsByPatient(patientId));
        }

        vitalSignDAO.softDelete(vitalId);
        // No AppEventType constant exists for a vital-sign deletion (only
        // VITAL_SIGN_RECORDED is defined) — per instructions we don't add new ones,
        // so no event is published here.
    }

    /**
     * Best-effort lookup of the owning patient for a given appointment, used only to
     * invalidate {@link CacheKey#vitalsByPatient(String)}. VitalSignDAO has no method for
     * this (it isn't part of its contract), and adding a patientId lookup would mean either
     * reaching into the appointment domain's DAO (owned by another workstream) or modifying
     * shared cache infrastructure — both out of scope here. A small read-only query against
     * `appointments` keeps this self-contained. Never throws: a failed lookup just means the
     * by-patient cache entry is left to expire on its own TTL instead of being evicted early.
     */
    private String resolvePatientId(String appointmentId) {
        String sql = "SELECT patient_id FROM appointments WHERE appointment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(appointmentId));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UUID patientId = rs.getObject("patient_id", UUID.class);
                    return patientId != null ? patientId.toString() : null;
                }
            }
        } catch (Exception e) {
            // Best-effort only — never block the write path on this lookup.
        }
        return null;
    }
}
