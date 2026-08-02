package hospital.management.backend.service.department;

import hospital.management.backend.dao.department.interfaces.DoctorScheduleDAO;
import hospital.management.backend.dto.doctor.CreateDoctorScheduleDTO;
import hospital.management.backend.dto.doctor.DoctorScheduleDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.doctor.DoctorSchedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Every doctorId/scheduleId used here is a fresh random UUID rather than a fixed
 * literal — findByDoctor() reads through a real, JVM-wide, static L1 in-process
 * cache keyed by doctorId with no reset hook exposed to tests, so a fixed id
 * would risk one test's cached DTO list leaking into another test's assertions
 * (see DoctorServiceImplTest for the same rationale).
 */
@ExtendWith(MockitoExtension.class)
class DoctorScheduleServiceImplTest {

    @Mock
    private DoctorScheduleDAO scheduleDAO;

    private DoctorScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DoctorScheduleServiceImpl(scheduleDAO);
    }

    private DoctorSchedule sampleSchedule(String id, String doctorId) {
        DoctorSchedule s = new DoctorSchedule();
        s.setScheduleId(id);
        s.setDoctorId(doctorId);
        s.setDayOfWeek("Mon");
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(17, 0));
        s.setIsAvailable(true);
        return s;
    }

    private CreateDoctorScheduleDTO sampleCreateDto(String doctorId) {
        return new CreateDoctorScheduleDTO(doctorId, "Mon", LocalTime.of(9, 0), LocalTime.of(17, 0), true);
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create throws IllegalArgumentException when doctorId is blank")
    void create_throwsIllegalArgumentException_whenDoctorIdBlank() {
        CreateDoctorScheduleDTO dto = new CreateDoctorScheduleDTO("  ", "Mon", LocalTime.of(9, 0), LocalTime.of(17, 0), true);

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        verifyNoInteractions(scheduleDAO);
    }

    @Test
    @DisplayName("create throws IllegalArgumentException when dayOfWeek is blank")
    void create_throwsIllegalArgumentException_whenDayOfWeekBlank() {
        String doctorId = UUID.randomUUID().toString();
        CreateDoctorScheduleDTO dto = new CreateDoctorScheduleDTO(doctorId, "  ", LocalTime.of(9, 0), LocalTime.of(17, 0), true);

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
    }

    @Test
    @DisplayName("create throws ValidationException when startTime or endTime is null — mirrors chk_schedule_time_order")
    void create_throwsValidationException_whenTimesNull() {
        String doctorId = UUID.randomUUID().toString();
        CreateDoctorScheduleDTO dto = new CreateDoctorScheduleDTO(doctorId, "Mon", null, LocalTime.of(17, 0), true);

        assertThrows(ValidationException.class, () -> service.create(dto));
        verifyNoInteractions(scheduleDAO);
    }

    @Test
    @DisplayName("create throws ValidationException when endTime is not after startTime — mirrors chk_schedule_time_order")
    void create_throwsValidationException_whenEndTimeNotAfterStartTime() {
        String doctorId = UUID.randomUUID().toString();
        CreateDoctorScheduleDTO dto = new CreateDoctorScheduleDTO(doctorId, "Mon", LocalTime.of(17, 0), LocalTime.of(9, 0), true);

        assertThrows(ValidationException.class, () -> service.create(dto));
        verifyNoInteractions(scheduleDAO);
    }

    @Test
    @DisplayName("create saves a new schedule with the validated fields when everything is valid")
    void create_savesSchedule_whenValid() throws Exception {
        String doctorId = UUID.randomUUID().toString();
        CreateDoctorScheduleDTO dto = sampleCreateDto(doctorId);
        when(scheduleDAO.save(any(DoctorSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorScheduleDTO result = service.create(dto);

        ArgumentCaptor<DoctorSchedule> captor = ArgumentCaptor.forClass(DoctorSchedule.class);
        verify(scheduleDAO).save(captor.capture());
        assertEquals("Mon", captor.getValue().getDayOfWeek());
        assertEquals(doctorId, result.getDoctorId());
    }

    // ── findByDoctor ──────────────────────────────────────────────────────

    @Test
    @DisplayName("findByDoctor maps every DAO schedule to a DTO")
    void findByDoctor_mapsEverySchedule() throws Exception {
        String doctorId = UUID.randomUUID().toString();
        DoctorSchedule mon = sampleSchedule(UUID.randomUUID().toString(), doctorId);
        DoctorSchedule tue = sampleSchedule(UUID.randomUUID().toString(), doctorId);
        tue.setDayOfWeek("Tue");
        when(scheduleDAO.findByDoctorId(doctorId)).thenReturn(List.of(mon, tue));

        List<DoctorScheduleDTO> result = service.findByDoctor(doctorId);

        assertEquals(2, result.size());
        assertEquals("Tue", result.get(1).getDayOfWeek());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update throws ResourceNotFoundException when the schedule doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() throws Exception {
        String scheduleId = UUID.randomUUID().toString();
        when(scheduleDAO.findById(scheduleId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(scheduleId, sampleCreateDto(UUID.randomUUID().toString())));
    }

    @Test
    @DisplayName("update throws ValidationException when the merged time range is invalid")
    void update_throwsValidationException_whenTimeOrderInvalid() throws Exception {
        String scheduleId = UUID.randomUUID().toString();
        String doctorId = UUID.randomUUID().toString();
        DoctorSchedule existing = sampleSchedule(scheduleId, doctorId);
        when(scheduleDAO.findById(scheduleId)).thenReturn(Optional.of(existing));

        CreateDoctorScheduleDTO dto = new CreateDoctorScheduleDTO(
                doctorId, "Mon", LocalTime.of(18, 0), LocalTime.of(9, 0), true);

        assertThrows(ValidationException.class, () -> service.update(scheduleId, dto));
        verify(scheduleDAO, never()).update(any());
    }

    @Test
    @DisplayName("update keeps the existing dayOfWeek/times/availability when the DTO leaves them null")
    void update_keepsExistingFields_whenDtoFieldsNull() throws Exception {
        String scheduleId = UUID.randomUUID().toString();
        String doctorId = UUID.randomUUID().toString();
        DoctorSchedule existing = sampleSchedule(scheduleId, doctorId);
        when(scheduleDAO.findById(scheduleId)).thenReturn(Optional.of(existing));
        when(scheduleDAO.update(any(DoctorSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateDoctorScheduleDTO dto = new CreateDoctorScheduleDTO(doctorId, null, null, null, null);

        DoctorScheduleDTO result = service.update(scheduleId, dto);

        assertEquals("Mon", result.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), result.getStartTime());
        assertEquals(LocalTime.of(17, 0), result.getEndTime());
        assertTrue(result.getIsAvailable());
    }

    @Test
    @DisplayName("update persists the new fields when everything is valid")
    void update_updatesFields_whenValid() throws Exception {
        String scheduleId = UUID.randomUUID().toString();
        String doctorId = UUID.randomUUID().toString();
        DoctorSchedule existing = sampleSchedule(scheduleId, doctorId);
        when(scheduleDAO.findById(scheduleId)).thenReturn(Optional.of(existing));
        when(scheduleDAO.update(any(DoctorSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateDoctorScheduleDTO dto = new CreateDoctorScheduleDTO(
                doctorId, "Wed", LocalTime.of(10, 0), LocalTime.of(14, 0), false);

        DoctorScheduleDTO result = service.update(scheduleId, dto);

        assertEquals("Wed", result.getDayOfWeek());
        assertEquals(LocalTime.of(10, 0), result.getStartTime());
        assertFalse(result.getIsAvailable());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete throws ResourceNotFoundException when the schedule doesn't exist")
    void delete_throwsResourceNotFoundException_whenMissing() throws Exception {
        String scheduleId = UUID.randomUUID().toString();
        when(scheduleDAO.findById(scheduleId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(scheduleId));
        verify(scheduleDAO, never()).softDelete(anyString());
    }

    @Test
    @DisplayName("delete soft-deletes an existing schedule")
    void delete_softDeletes_whenFound() throws Exception {
        String scheduleId = UUID.randomUUID().toString();
        String doctorId = UUID.randomUUID().toString();
        when(scheduleDAO.findById(scheduleId)).thenReturn(Optional.of(sampleSchedule(scheduleId, doctorId)));

        service.delete(scheduleId);

        verify(scheduleDAO).softDelete(scheduleId);
    }
}
