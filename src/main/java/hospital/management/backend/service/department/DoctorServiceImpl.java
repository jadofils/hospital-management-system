package hospital.management.backend.service.department;

import com.fasterxml.jackson.core.type.TypeReference;
import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.department.interfaces.DepartmentDAO;
import hospital.management.backend.dao.department.interfaces.DoctorDAO;
import hospital.management.backend.dto.doctor.CreateDoctorDTO;
import hospital.management.backend.dto.doctor.DoctorDTO;
import hospital.management.backend.dto.doctor.DoctorSummaryDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.doctor.DoctorMapper;
import hospital.management.backend.model.doctor.Department;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.service.department.interfaces.DoctorService;
import hospital.management.backend.utils.AlgorithmUtils;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DoctorServiceImpl implements DoctorService {

    private final DoctorDAO     doctorDAO;
    private final DepartmentDAO departmentDAO;

    public DoctorServiceImpl(DoctorDAO doctorDAO, DepartmentDAO departmentDAO) {
        this.doctorDAO     = doctorDAO;
        this.departmentDAO = departmentDAO;
    }

    @Override
    public DoctorDTO create(CreateDoctorDTO dto) throws Exception {
        ValidatorUtils.requireValidName(dto.getFirstName(), "First name");
        ValidatorUtils.requireValidName(dto.getLastName(), "Last name");

        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            ValidatorUtils.requireValidPhone(dto.getPhone().trim(), "Phone");
        }
        if (dto.getSpecialization() != null) {
            ValidatorUtils.requireNonBlankMaxLength(dto.getSpecialization(), 200, "Specialization");
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            ValidatorUtils.requireValidEmail(dto.getEmail(), "email");
            if (doctorDAO.findByEmail(dto.getEmail()).isPresent()) {
                throw new ValidationException("email", "Email \"" + dto.getEmail() + "\" is already registered.");
            }
        }
        if (dto.getDepartmentId() != null && !dto.getDepartmentId().isBlank()) {
            departmentDAO.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", dto.getDepartmentId()));
        }

        // Delete-before-write: invalidate every cached doctor list/lookup before persisting.
        CacheService.evictByPattern(CacheKey.ALL_DOCTORS);
        Doctor saved = doctorDAO.save(DoctorMapper.toEntity(dto));
        EventBus.publish(AppEventType.DOCTOR_CREATED, saved.getDoctorId());
        return DoctorMapper.toDTO(saved);
    }

    @Override
    public DoctorDTO findById(String doctorId) throws Exception {
        Optional<DoctorDTO> cached = CacheService.get(CacheKey.doctor(doctorId), DoctorDTO.class);
        if (cached.isPresent()) return cached.get();

        Doctor doctor = doctorDAO.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        DoctorDTO dto = DoctorMapper.toDTO(doctor);
        CacheService.set(CacheKey.doctor(doctorId), dto, CacheDomain.DOCTOR);
        return dto;
    }

    @Override
    public DoctorDTO findByEmail(String email) throws Exception {
        if (email == null || email.isBlank()) {
            throw new ValidationException("email", "Email cannot be blank");
        }
        Doctor doctor = doctorDAO.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor with email", email));
        return DoctorMapper.toDTO(doctor);
    }

    @Override
    public PageResult<DoctorDTO> findAll(PageRequest request) throws Exception {
        // Cursor-based pagination has no stable page number to key a cache entry on
        // (CacheKey.doctorList(page, size) assumes classic offset paging), so this
        // follows the same uncached pass-through as UserServiceImpl.findAll.
        return doctorDAO.findAll(request).map(DoctorMapper::toDTO);
    }

    @Override
    public List<DoctorSummaryDTO> findByDepartment(String departmentId) throws Exception {
        Optional<List<DoctorSummaryDTO>> cached = CacheService.get(
                CacheKey.doctorsByDept(departmentId),
                new TypeReference<List<DoctorSummaryDTO>>() {});
        if (cached.isPresent()) return cached.get();

        String departmentName = departmentDAO.findById(departmentId)
                .map(Department::getName)
                .orElse(null);

        List<DoctorSummaryDTO> dtos = new ArrayList<>();
        for (Doctor doctor : doctorDAO.findByDepartmentId(departmentId)) {
            dtos.add(DoctorMapper.toSummaryDTO(doctor, departmentName));
        }

        // Sort alphabetically by full name using in-memory merge sort (O(n log n), stable).
        // Guarantees consistent ordering in the cached list regardless of DB insertion order.
        AlgorithmUtils.mergeSort(dtos,
                Comparator.comparing(DoctorSummaryDTO::getFullName, String.CASE_INSENSITIVE_ORDER));

        CacheService.set(CacheKey.doctorsByDept(departmentId), dtos, CacheDomain.DOCTOR);
        return dtos;
    }

    /**
     * Locates a doctor by ID within an already-cached department list using binary search.
     *
     * The list is re-sorted by doctorId before searching so the O(log n) binary
     * search precondition is satisfied. Returns Optional.empty() if the department
     * list is not cached or the doctor is not found — the caller should fall back
     * to {@link #findById(String)} in that case.
     */
    public Optional<DoctorSummaryDTO> findDoctorInCachedDepartment(String departmentId,
                                                                    String targetDoctorId) {
        Optional<List<DoctorSummaryDTO>> cached = CacheService.get(
                CacheKey.doctorsByDept(departmentId),
                new TypeReference<List<DoctorSummaryDTO>>() {});
        if (cached.isEmpty()) return Optional.empty();

        List<DoctorSummaryDTO> sorted = new ArrayList<>(cached.get());
        AlgorithmUtils.mergeSort(sorted, Comparator.comparing(DoctorSummaryDTO::getDoctorId));
        int idx = AlgorithmUtils.binarySearch(sorted, targetDoctorId, DoctorSummaryDTO::getDoctorId);
        return idx >= 0 ? Optional.of(sorted.get(idx)) : Optional.empty();
    }

    @Override
    public DoctorDTO update(String doctorId, CreateDoctorDTO dto) throws Exception {
        Doctor doctor = doctorDAO.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        ValidatorUtils.requireValidName(dto.getFirstName(), "First name");
        ValidatorUtils.requireValidName(dto.getLastName(), "Last name");
        String firstName = dto.getFirstName().trim();
        String lastName  = dto.getLastName().trim();

        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            ValidatorUtils.requireValidPhone(dto.getPhone().trim(), "Phone");
        }
        if (dto.getSpecialization() != null) {
            ValidatorUtils.requireNonBlankMaxLength(dto.getSpecialization(), 200, "Specialization");
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank() && !dto.getEmail().equals(doctor.getEmail())) {
            ValidatorUtils.requireValidEmail(dto.getEmail(), "email");
            if (doctorDAO.findByEmail(dto.getEmail()).isPresent()) {
                throw new ValidationException("email", "Email \"" + dto.getEmail() + "\" is already registered.");
            }
        }
        if (dto.getDepartmentId() != null && !dto.getDepartmentId().isBlank()
                && !dto.getDepartmentId().equals(doctor.getDepartmentId())) {
            departmentDAO.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", dto.getDepartmentId()));
        }

        String oldDepartmentId = doctor.getDepartmentId();
        doctor.setFirstName(firstName);
        doctor.setLastName(lastName);
        doctor.setSpecialization(dto.getSpecialization());
        doctor.setPhone(dto.getPhone());
        doctor.setEmail(dto.getEmail());
        doctor.setDepartmentId(dto.getDepartmentId());

        CacheService.evict(CacheKey.doctor(doctorId));
        CacheService.evictByPattern(CacheKey.ALL_DOCTORS);
        if (oldDepartmentId != null) CacheService.evict(CacheKey.doctorsByDept(oldDepartmentId));
        if (dto.getDepartmentId() != null) CacheService.evict(CacheKey.doctorsByDept(dto.getDepartmentId()));

        Doctor saved = doctorDAO.update(doctor);
        EventBus.publish(AppEventType.DOCTOR_UPDATED, doctorId);
        return DoctorMapper.toDTO(saved);
    }

    @Override
    public void delete(String doctorId) throws Exception {
        Doctor doctor = doctorDAO.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        CacheService.evict(CacheKey.doctor(doctorId));
        CacheService.evictByPattern(CacheKey.ALL_DOCTORS);
        if (doctor.getDepartmentId() != null) CacheService.evict(CacheKey.doctorsByDept(doctor.getDepartmentId()));

        doctorDAO.softDelete(doctorId);
        EventBus.publish(AppEventType.DOCTOR_DELETED, doctorId);
    }
}
