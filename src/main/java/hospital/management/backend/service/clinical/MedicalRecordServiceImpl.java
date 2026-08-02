package hospital.management.backend.service.clinical;

import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.clinical.interfaces.MedicalRecordDAO;
import hospital.management.backend.dto.clinical.CreateMedicalRecordDTO;
import hospital.management.backend.dto.clinical.MedicalRecordDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.clinical.MedicalRecordMapper;
import hospital.management.backend.model.patient.MedicalRecord;
import hospital.management.backend.service.clinical.interfaces.MedicalRecordService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.util.Optional;

public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordDAO recordDAO;

    public MedicalRecordServiceImpl(MedicalRecordDAO recordDAO) {
        this.recordDAO = recordDAO;
    }

    @Override
    public MedicalRecordDTO create(CreateMedicalRecordDTO dto) throws Exception {
        String appointmentId = ValidatorUtils.requireNonBlank(dto.getAppointmentId(), "appointmentId");
        if (recordDAO.findByAppointmentId(appointmentId).isPresent()) {
            throw new ValidationException("appointmentId",
                "A medical record already exists for appointment " + appointmentId + ".");
        }

        // Single INSERT is already atomic — no TransactionManager needed here.
        CacheService.evictByPattern(CacheKey.ALL_RECORDS);
        MedicalRecord saved = recordDAO.save(MedicalRecordMapper.toEntity(dto));
        EventBus.publish(AppEventType.MEDICAL_RECORD_CREATED, saved.getRecordId());
        return MedicalRecordMapper.toDTO(saved);
    }

    @Override
    public MedicalRecordDTO findById(String recordId) throws Exception {
        Optional<MedicalRecordDTO> cached = CacheService.get(CacheKey.medicalRecord(recordId), MedicalRecordDTO.class);
        if (cached.isPresent()) return cached.get();

        MedicalRecord record = recordDAO.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", recordId));
        MedicalRecordDTO dto = MedicalRecordMapper.toDTO(record);
        CacheService.set(CacheKey.medicalRecord(recordId), dto, CacheDomain.MEDICAL_RECORD);
        return dto;
    }

    @Override
    public MedicalRecordDTO findByAppointment(String appointmentId) throws Exception {
        Optional<MedicalRecordDTO> cached = CacheService.get(CacheKey.recordByAppt(appointmentId), MedicalRecordDTO.class);
        if (cached.isPresent()) return cached.get();

        MedicalRecord record = recordDAO.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", appointmentId));
        MedicalRecordDTO dto = MedicalRecordMapper.toDTO(record);
        CacheService.set(CacheKey.recordByAppt(appointmentId), dto, CacheDomain.MEDICAL_RECORD);
        return dto;
    }

    @Override
    public MedicalRecordDTO update(String recordId, CreateMedicalRecordDTO dto) throws Exception {
        MedicalRecord record = recordDAO.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", recordId));

        if (dto.getDiagnosis() != null) record.setDiagnosis(dto.getDiagnosis());
        if (dto.getSymptoms() != null) record.setSymptoms(dto.getSymptoms());
        if (dto.getNotes() != null) record.setNotes(dto.getNotes());

        CacheService.evict(CacheKey.medicalRecord(recordId));
        CacheService.evict(CacheKey.recordByAppt(record.getAppointmentId()));
        CacheService.evictByPattern(CacheKey.ALL_RECORDS);

        MedicalRecord saved = recordDAO.update(record);
        EventBus.publish(AppEventType.MEDICAL_RECORD_UPDATED, saved.getRecordId());
        return MedicalRecordMapper.toDTO(saved);
    }

    @Override
    public void delete(String recordId) throws Exception {
        MedicalRecord record = recordDAO.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", recordId));

        CacheService.evict(CacheKey.medicalRecord(recordId));
        CacheService.evict(CacheKey.recordByAppt(record.getAppointmentId()));
        CacheService.evictByPattern(CacheKey.ALL_RECORDS);

        recordDAO.softDelete(recordId);
    }
}