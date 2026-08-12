package hospital.management.backend.service.department;

import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.department.interfaces.DepartmentDAO;
import hospital.management.backend.dto.doctor.CreateDepartmentDTO;
import hospital.management.backend.dto.doctor.DepartmentDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.doctor.Department;
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
 * findById()/CacheService.get() reads through a real, JVM-wide, static L1
 * in-process cache with no reset hook exposed to tests, so a fixed id would
 * risk one test's cached DTO leaking into another test's assertions (see
 * DoctorServiceImplTest for the same rationale).
 *
 * findAll() is the one exception: DepartmentServiceImpl caches it under the
 * fixed key "department:list" (no per-call parameter), so the list-returning
 * test explicitly evicts that key first to guarantee it reads through to the
 * mocked DAO rather than a stale value left by another test/run in this JVM.
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentDAO departmentDAO;

    private DepartmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DepartmentServiceImpl(departmentDAO);
    }

    private Department sampleDepartment(String id, String name) {
        Department d = new Department();
        d.setDepartmentId(id);
        d.setName(name);
        d.setLocation("Building A");
        d.setPhone("+15551234567");
        return d;
    }

    private CreateDepartmentDTO sampleCreateDto(String name) {
        return new CreateDepartmentDTO(name, "Building A", "+15551234567");
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create throws IllegalArgumentException when name is blank")
    void create_throwsIllegalArgumentException_whenNameBlank() {
        CreateDepartmentDTO dto = sampleCreateDto("   ");

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        verifyNoInteractions(departmentDAO);
    }

    @Test
    @DisplayName("create throws IllegalArgumentException when name is entirely digits")
    void create_throwsIllegalArgumentException_whenNamePureNumeric() {
        CreateDepartmentDTO dto = sampleCreateDto("12345");

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        verifyNoInteractions(departmentDAO);
    }

    @Test
    @DisplayName("create throws ValidationException when a department with the same name already exists")
    void create_throwsValidationException_whenNameAlreadyExists() throws Exception {
        CreateDepartmentDTO dto = sampleCreateDto("Cardiology");
        when(departmentDAO.findByName("Cardiology"))
                .thenReturn(Optional.of(sampleDepartment(UUID.randomUUID().toString(), "Cardiology")));

        assertThrows(ValidationException.class, () -> service.create(dto));
        verify(departmentDAO, never()).save(any());
    }

    @Test
    @DisplayName("create saves a new department with the validated fields when the name is unique")
    void create_savesDepartment_whenNameUnique() throws Exception {
        CreateDepartmentDTO dto = sampleCreateDto("Cardiology");
        when(departmentDAO.findByName("Cardiology")).thenReturn(Optional.empty());
        when(departmentDAO.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentDTO result = service.create(dto);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentDAO).save(captor.capture());
        assertEquals("Cardiology", captor.getValue().getName());
        assertEquals("Cardiology", result.getName());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns a mapped DTO when the DAO finds a matching department")
    void findById_returnsMappedDto_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(departmentDAO.findById(id)).thenReturn(Optional.of(sampleDepartment(id, "Cardiology")));

        DepartmentDTO dto = service.findById(id);

        assertEquals(id, dto.getDepartmentId());
        assertEquals("Cardiology", dto.getName());
        verify(departmentDAO).findById(id);
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the DAO finds nothing")
    void findById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(departmentDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll maps every DAO department to a DTO")
    void findAll_mapsEveryDepartment() throws Exception {
        CacheService.evict(CacheKey.departmentList());
        when(departmentDAO.findAll()).thenReturn(List.of(
                sampleDepartment(UUID.randomUUID().toString(), "Cardiology"),
                sampleDepartment(UUID.randomUUID().toString(), "Neurology")));

        List<DepartmentDTO> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals("Neurology", result.get(1).getName());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update throws ResourceNotFoundException when the department doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(departmentDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(id, sampleCreateDto("Cardiology")));
    }

    @Test
    @DisplayName("update throws ValidationException when renaming to a name already used by another department")
    void update_throwsValidationException_whenNewNameTaken() throws Exception {
        String id = UUID.randomUUID().toString();
        Department existing = sampleDepartment(id, "Cardiology");
        when(departmentDAO.findById(id)).thenReturn(Optional.of(existing));
        when(departmentDAO.findByName("Neurology"))
                .thenReturn(Optional.of(sampleDepartment(UUID.randomUUID().toString(), "Neurology")));

        assertThrows(ValidationException.class, () -> service.update(id, sampleCreateDto("Neurology")));
        verify(departmentDAO, never()).update(any());
    }

    @Test
    @DisplayName("update allows keeping the department's own current name unchanged without a conflict check")
    void update_allowsKeepingOwnName() throws Exception {
        String id = UUID.randomUUID().toString();
        Department existing = sampleDepartment(id, "Cardiology");
        when(departmentDAO.findById(id)).thenReturn(Optional.of(existing));
        when(departmentDAO.update(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateDepartmentDTO dto = new CreateDepartmentDTO("Cardiology", "Building B", "+15559998888");
        DepartmentDTO result = service.update(id, dto);

        assertEquals("Building B", result.getLocation());
        verify(departmentDAO, never()).findByName(anyString());
    }

    @Test
    @DisplayName("update persists the new fields when everything is valid")
    void update_updatesFields_whenValid() throws Exception {
        String id = UUID.randomUUID().toString();
        Department existing = sampleDepartment(id, "Cardiology");
        when(departmentDAO.findById(id)).thenReturn(Optional.of(existing));
        when(departmentDAO.findByName("Neurology")).thenReturn(Optional.empty());
        when(departmentDAO.update(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentDTO result = service.update(id, sampleCreateDto("Neurology"));

        assertEquals("Neurology", result.getName());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete soft-deletes the department via the DAO")
    void delete_softDeletes() throws Exception {
        String id = UUID.randomUUID().toString();

        service.delete(id);

        verify(departmentDAO).softDelete(id);
    }

    @Test
    @DisplayName("delete propagates ResourceNotFoundException from the DAO — the service does not pre-check existence")
    void delete_propagatesResourceNotFoundException_whenDaoThrows() throws Exception {
        String id = UUID.randomUUID().toString();
        doThrow(new ResourceNotFoundException("Department", id)).when(departmentDAO).softDelete(id);

        assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
    }
}
