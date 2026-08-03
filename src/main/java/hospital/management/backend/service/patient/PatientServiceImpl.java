package hospital.management.backend.service.patient;

import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.patient.interfaces.PatientDAO;
import hospital.management.backend.dto.patient.CreatePatientDTO;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.patient.PatientSummaryDTO;
import hospital.management.backend.dto.patient.UpdatePatientDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.patient.PatientMapper;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.service.patient.interfaces.PatientService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.backend.service.log.ServiceAudit;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.Optional;

public class PatientServiceImpl implements PatientService {

    private final PatientDAO patientDAO;

    public PatientServiceImpl(PatientDAO patientDAO) {
        this.patientDAO = patientDAO;
    }

    @Override
    public PatientDTO create(CreatePatientDTO dto) throws Exception {
        String firstName = ValidatorUtils.requireNonBlank(dto.getFirstName(), "firstName");
        String lastName  = ValidatorUtils.requireNonBlank(dto.getLastName(), "lastName");
        if (dto.getDob() == null) {
            throw new ValidationException("dob", "Date of birth is required.");
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            ValidatorUtils.requireValidEmail(dto.getEmail(), "email");
            if (patientDAO.findByEmail(dto.getEmail()).isPresent()) {
                throw new ValidationException("email", "Email \"" + dto.getEmail() + "\" is already registered.");
            }
        }

        Patient patient = PatientMapper.toEntity(dto);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);

        // A single INSERT is already atomic under Postgres — no TransactionManager needed
        // for this plain single-table write.
        CacheService.evictByPattern(CacheKey.ALL_PATIENTS);
        Patient saved = patientDAO.save(patient);
        ServiceAudit.record("patients", "create", saved.getPatientId());
        EventBus.publish(AppEventType.PATIENT_CREATED, saved.getPatientId());
        return PatientMapper.toDTO(saved);
    }

    @Override
    public PatientDTO findById(String patientId) throws Exception {
        Optional<PatientDTO> cached = CacheService.get(CacheKey.patient(patientId), PatientDTO.class);
        if (cached.isPresent()) return cached.get();

        Patient patient = patientDAO.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
        PatientDTO dto = PatientMapper.toDTO(patient);
        CacheService.set(CacheKey.patient(patientId), dto, CacheDomain.PATIENT);
        return dto;
    }

    @Override
    public PageResult<PatientDTO> findAll(PageRequest request) throws Exception {
        // Cursor-paginated results are intentionally not cached here — PageResult's
        // constructor is package-private to hospital.management.backend.utils.pagination,
        // so a service outside that package can only ever produce one via
        // CursorPagination.toResult(...) or by .map()-ing an existing instance, never by
        // rehydrating one from a cached blob. This mirrors UserServiceImpl.findAll, the
        // reference implementation for cursor-paginated lists, which also does not cache.
        return patientDAO.findAll(request).map(PatientMapper::toDTO);
    }

    @Override
    public PageResult<PatientSummaryDTO> search(String query, PageRequest request) throws Exception {
        // See findAll() above for why the PageResult wrapper itself isn't cached.
        return patientDAO.search(query, request).map(PatientMapper::toSummaryDTO);
    }

    @Override
    public PatientDTO update(UpdatePatientDTO dto) throws Exception {
        String patientId = ValidatorUtils.requireNonBlank(dto.getPatientId(), "patientId");
        Patient patient = patientDAO.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            ValidatorUtils.requireValidEmail(dto.getEmail(), "email");
            if (!dto.getEmail().equals(patient.getEmail()) && patientDAO.findByEmail(dto.getEmail()).isPresent()) {
                throw new ValidationException("email", "Email \"" + dto.getEmail() + "\" is already registered.");
            }
            patient.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) patient.setPhone(dto.getPhone());
        if (dto.getAddress() != null) patient.setAddress(dto.getAddress());

        // Delete-before-write: evict first so a concurrent reader never sees a stale hit.
        CacheService.evict(CacheKey.patient(patientId));
        CacheService.evictByPattern(CacheKey.ALL_PATIENTS);
        Patient saved = patientDAO.update(patient);
        ServiceAudit.record("patients", "update", patientId);
        EventBus.publish(AppEventType.PATIENT_UPDATED, patientId);
        return PatientMapper.toDTO(saved);
    }

    @Override
    public void delete(String patientId) throws Exception {
        CacheService.evict(CacheKey.patient(patientId));
        CacheService.evictByPattern(CacheKey.ALL_PATIENTS);
        patientDAO.softDelete(patientId);
        ServiceAudit.record("patients", "delete", patientId);
        EventBus.publish(AppEventType.PATIENT_DELETED, patientId);
    }
}
