package hospital.management.backend.service.department;

import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.department.interfaces.DepartmentDAO;
import hospital.management.backend.dto.doctor.CreateDepartmentDTO;
import hospital.management.backend.dto.doctor.DepartmentDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.doctor.DepartmentMapper;
import hospital.management.backend.model.doctor.Department;
import hospital.management.backend.service.department.interfaces.DepartmentService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentDAO departmentDAO;

    public DepartmentServiceImpl(DepartmentDAO departmentDAO) {
        this.departmentDAO = departmentDAO;
    }

    @Override
    public DepartmentDTO create(CreateDepartmentDTO dto) throws Exception {
        String name = ValidatorUtils.requireNonBlank(dto.getName(), "name");
        if (departmentDAO.findByName(name).isPresent()) {
            throw new ValidationException("name", "Department \"" + name + "\" already exists.");
        }

        // A single INSERT is already atomic — no TransactionManager needed for
        // plain single-table CRUD like this (unlike e.g. user creation + role
        // assignment, which touches two tables and must succeed together).
        CacheService.evict(CacheKey.departmentList());
        Department saved = departmentDAO.save(DepartmentMapper.toEntity(dto));
        EventBus.publish(AppEventType.DEPARTMENT_CREATED, saved.getDepartmentId());
        return DepartmentMapper.toDTO(saved);
    }

    @Override
    public DepartmentDTO findById(String departmentId) throws Exception {
        Optional<DepartmentDTO> cached = CacheService.get(CacheKey.department(departmentId), DepartmentDTO.class);
        if (cached.isPresent()) return cached.get();

        Department department = departmentDAO.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", departmentId));
        DepartmentDTO dto = DepartmentMapper.toDTO(department);
        CacheService.set(CacheKey.department(departmentId), dto, CacheDomain.DEPARTMENT);
        return dto;
    }

    @Override
    public List<DepartmentDTO> findAll() throws Exception {
        Optional<List<DepartmentDTO>> cached = CacheService.get(
            CacheKey.departmentList(),
            new com.fasterxml.jackson.core.type.TypeReference<List<DepartmentDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<DepartmentDTO> dtos = new ArrayList<>();
        for (Department department : departmentDAO.findAll()) dtos.add(DepartmentMapper.toDTO(department));
        CacheService.set(CacheKey.departmentList(), dtos, CacheDomain.DEPARTMENT);
        return dtos;
    }

    @Override
    public DepartmentDTO update(String departmentId, CreateDepartmentDTO dto) throws Exception {
        Department department = departmentDAO.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", departmentId));

        String name = ValidatorUtils.requireNonBlank(dto.getName(), "name");
        if (!name.equals(department.getName()) && departmentDAO.findByName(name).isPresent()) {
            throw new ValidationException("name", "Department \"" + name + "\" already exists.");
        }

        department.setName(name);
        department.setLocation(dto.getLocation());
        department.setPhone(dto.getPhone());

        CacheService.evict(CacheKey.department(departmentId));
        CacheService.evict(CacheKey.departmentList());
        Department saved = departmentDAO.update(department);
        EventBus.publish(AppEventType.DEPARTMENT_UPDATED, departmentId);
        return DepartmentMapper.toDTO(saved);
    }

    @Override
    public void delete(String departmentId) throws Exception {
        CacheService.evict(CacheKey.department(departmentId));
        CacheService.evict(CacheKey.departmentList());
        departmentDAO.softDelete(departmentId);
        EventBus.publish(AppEventType.DEPARTMENT_DELETED, departmentId);
    }
}
