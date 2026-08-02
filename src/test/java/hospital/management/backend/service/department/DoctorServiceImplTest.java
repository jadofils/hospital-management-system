package hospital.management.backend.service.department;

import hospital.management.backend.dao.department.interfaces.DepartmentDAO;
import hospital.management.backend.dao.department.interfaces.DoctorDAO;
import hospital.management.backend.dto.doctor.CreateDoctorDTO;
import hospital.management.backend.dto.doctor.DoctorDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.doctor.Department;
import hospital.management.backend.model.doctor.Doctor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Every id used here is a fresh random UUID rather than a fixed literal —
 * findById()/CacheService.get() reads through a real, JVM-wide, static L1
 * in-process cache with no reset hook exposed to tests, so a fixed id would
 * risk one test's cached DTO leaking into another test's assertions.
 */
@ExtendWith(MockitoExtension.class)
class DoctorServiceImplTest {

    @Mock
    private DoctorDAO doctorDAO;
    @Mock
    private DepartmentDAO departmentDAO;

    private DoctorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DoctorServiceImpl(doctorDAO, departmentDAO);
    }

    private Doctor sampleDoctor(String id, String departmentId) {
        Doctor d = new Doctor();
        d.setDoctorId(id);
        d.setDepartmentId(departmentId);
        d.setFirstName("Sarah");
        d.setLastName("Chen");
        d.setSpecialization("Cardiology");
        d.setEmail("sarah.chen@example.com");
        return d;
    }

    private CreateDoctorDTO sampleCreateDto(String departmentId, String email) {
        return new CreateDoctorDTO(departmentId, "Sarah", "Chen", "Cardiology", "+15551112222", email);
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns a mapped DTO when the DAO finds a matching doctor")
    void findById_returnsMappedDto_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(doctorDAO.findById(id)).thenReturn(Optional.of(sampleDoctor(id, null)));

        DoctorDTO dto = service.findById(id);

        assertEquals(id, dto.getDoctorId());
        assertEquals("Sarah", dto.getFirstName());
        verify(doctorDAO).findById(id);
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the DAO finds nothing")
    void findById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(doctorDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create throws IllegalArgumentException when firstName is blank")
    void create_throwsIllegalArgumentException_whenFirstNameBlank() {
        CreateDoctorDTO dto = new CreateDoctorDTO(null, "  ", "Chen", "Cardiology", null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        verifyNoInteractions(doctorDAO);
    }

    @Test
    @DisplayName("create throws IllegalArgumentException when the email is malformed")
    void create_throwsIllegalArgumentException_whenEmailMalformed() {
        CreateDoctorDTO dto = sampleCreateDto(null, "not-an-email");

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
    }

    @Test
    @DisplayName("create throws ValidationException when the email is already registered to another doctor")
    void create_throwsValidationException_whenEmailAlreadyRegistered() throws Exception {
        CreateDoctorDTO dto = sampleCreateDto(null, "sarah.chen@example.com");
        when(doctorDAO.findByEmail("sarah.chen@example.com"))
                .thenReturn(Optional.of(sampleDoctor(UUID.randomUUID().toString(), null)));

        assertThrows(ValidationException.class, () -> service.create(dto));
        verify(doctorDAO, never()).save(any());
    }

    @Test
    @DisplayName("create throws ResourceNotFoundException when the given department doesn't exist")
    void create_throwsResourceNotFoundException_whenDepartmentMissing() throws Exception {
        String departmentId = UUID.randomUUID().toString();
        CreateDoctorDTO dto = sampleCreateDto(departmentId, null);
        when(departmentDAO.findById(departmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(dto));
        verify(doctorDAO, never()).save(any());
    }

    @Test
    @DisplayName("create saves a new doctor with the validated fields when everything is valid")
    void create_savesDoctor_whenValid() throws Exception {
        String departmentId = UUID.randomUUID().toString();
        CreateDoctorDTO dto = sampleCreateDto(departmentId, "sarah.chen@example.com");
        when(departmentDAO.findById(departmentId))
                .thenReturn(Optional.of(new Department(departmentId, "Cardiology Dept", null, null, null, null, null)));
        when(doctorDAO.findByEmail("sarah.chen@example.com")).thenReturn(Optional.empty());
        when(doctorDAO.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorDTO result = service.create(dto);

        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorDAO).save(captor.capture());
        assertEquals("Sarah", captor.getValue().getFirstName());
        assertEquals(departmentId, result.getDepartmentId());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update throws ResourceNotFoundException when the doctor doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(doctorDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(id, sampleCreateDto(null, null)));
    }

    @Test
    @DisplayName("update throws ValidationException when changing to an email already used by someone else")
    void update_throwsValidationException_whenEmailTakenBySomeoneElse() throws Exception {
        String id = UUID.randomUUID().toString();
        Doctor existing = sampleDoctor(id, null); // email = sarah.chen@example.com
        when(doctorDAO.findById(id)).thenReturn(Optional.of(existing));
        when(doctorDAO.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(sampleDoctor(UUID.randomUUID().toString(), null)));

        CreateDoctorDTO dto = new CreateDoctorDTO(null, "Sarah", "Chen", "Cardiology", null, "taken@example.com");

        assertThrows(ValidationException.class, () -> service.update(id, dto));
        verify(doctorDAO, never()).update(any());
    }

    @Test
    @DisplayName("update allows keeping the doctor's own current email unchanged without a conflict check")
    void update_allowsKeepingOwnEmail() throws Exception {
        String id = UUID.randomUUID().toString();
        Doctor existing = sampleDoctor(id, null); // email = sarah.chen@example.com
        when(doctorDAO.findById(id)).thenReturn(Optional.of(existing));
        when(doctorDAO.update(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateDoctorDTO dto = new CreateDoctorDTO(
                null, "Sarah", "Chen", "Neurology", "+15559998888", "sarah.chen@example.com");

        DoctorDTO result = service.update(id, dto);

        assertEquals("Neurology", result.getSpecialization());
        verify(doctorDAO, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException when moving the doctor to a non-existent department")
    void update_throwsResourceNotFoundException_whenNewDepartmentMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        String newDepartmentId = UUID.randomUUID().toString();
        Doctor existing = sampleDoctor(id, null);
        when(doctorDAO.findById(id)).thenReturn(Optional.of(existing));
        when(departmentDAO.findById(newDepartmentId)).thenReturn(Optional.empty());

        CreateDoctorDTO dto = new CreateDoctorDTO(
                newDepartmentId, "Sarah", "Chen", "Cardiology", null, "sarah.chen@example.com");

        assertThrows(ResourceNotFoundException.class, () -> service.update(id, dto));
        verify(doctorDAO, never()).update(any());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete throws ResourceNotFoundException when the doctor doesn't exist")
    void delete_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(doctorDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
        verify(doctorDAO, never()).softDelete(anyString());
    }

    @Test
    @DisplayName("delete soft-deletes an existing doctor")
    void delete_softDeletes_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(doctorDAO.findById(id)).thenReturn(Optional.of(sampleDoctor(id, null)));

        service.delete(id);

        verify(doctorDAO).softDelete(id);
    }
}
