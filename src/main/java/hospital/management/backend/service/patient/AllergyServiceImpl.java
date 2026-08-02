package hospital.management.backend.service.patient;

import com.fasterxml.jackson.core.type.TypeReference;
import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.patient.interfaces.PatientAllergyDAO;
import hospital.management.backend.dto.patient.CreatePatientAllergyDTO;
import hospital.management.backend.dto.patient.PatientAllergyDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.mapper.patient.PatientAllergyMapper;
import hospital.management.backend.model.enums.AllergySeverity;
import hospital.management.backend.model.patient.PatientAllergy;
import hospital.management.backend.service.patient.interfaces.AllergyService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AllergyServiceImpl implements AllergyService {

    private final PatientAllergyDAO allergyDAO;

    public AllergyServiceImpl(PatientAllergyDAO allergyDAO) {
        this.allergyDAO = allergyDAO;
    }

    @Override
    public PatientAllergyDTO add(CreatePatientAllergyDTO dto) throws Exception {
        String patientId = ValidatorUtils.requireNonBlank(dto.getPatientId(), "patientId");
        ValidatorUtils.requireValidUuid(patientId, "patientId");
        ValidatorUtils.requireNonBlank(dto.getAllergen(), "allergen");
        if (dto.getSeverity() != null && !dto.getSeverity().isBlank()) {
            // Validates against the DB CHECK constraint ('mild'/'moderate'/'severe');
            // throws IllegalArgumentException on an unrecognised value.
            AllergySeverity.fromDbValue(dto.getSeverity());
        }

        // A single INSERT is already atomic — no TransactionManager needed here.
        CacheService.evict(CacheKey.allergies(patientId));
        PatientAllergy saved = allergyDAO.save(PatientAllergyMapper.toEntity(dto));
        EventBus.publish(AppEventType.PATIENT_ALLERGY_ADDED, saved.getAllergyId());
        return PatientAllergyMapper.toDTO(saved);
    }

    @Override
    public List<PatientAllergyDTO> findByPatient(String patientId) throws Exception {
        Optional<List<PatientAllergyDTO>> cached = CacheService.get(
            CacheKey.allergies(patientId),
            new TypeReference<List<PatientAllergyDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<PatientAllergyDTO> dtos = new ArrayList<>();
        for (PatientAllergy allergy : allergyDAO.findByPatientId(patientId)) {
            dtos.add(PatientAllergyMapper.toDTO(allergy));
        }
        CacheService.set(CacheKey.allergies(patientId), dtos, CacheDomain.PATIENT);
        return dtos;
    }

    @Override
    public void delete(String allergyId) throws Exception {
        PatientAllergy allergy = allergyDAO.findById(allergyId)
                .orElseThrow(() -> new ResourceNotFoundException("PatientAllergy", allergyId));

        CacheService.evict(CacheKey.allergies(allergy.getPatientId()));
        allergyDAO.softDelete(allergyId);
        EventBus.publish(AppEventType.PATIENT_ALLERGY_REMOVED, allergyId);
    }
}