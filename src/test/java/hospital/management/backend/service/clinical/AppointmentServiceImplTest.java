package hospital.management.backend.service.clinical;

import hospital.management.backend.dao.clinical.interfaces.AppointmentDAO;
import hospital.management.backend.dao.department.interfaces.DoctorDAO;
import hospital.management.backend.dao.patient.interfaces.PatientDAO;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.clinical.CreateAppointmentDTO;
import hospital.management.backend.dto.clinical.UpdateAppointmentDTO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Every id used here is a fresh random UUID rather than a fixed literal — findById(),
 * findByPatient() and findByDoctor() each read through a real, JVM-wide, static L1
 * in-process cache with no reset hook exposed to tests, so a fixed id would risk one
 * test's cached DTO leaking into another test's assertions. CacheService's Redis (L2)
 * tier is left un-mocked: with no Redis reachable in this test process every call is a
 * guaranteed cache miss (caught and logged internally, never propagated to the caller —
 * see CacheService's own javadoc), so every test still exercises the DAO as expected.
 * Neither book()/update()/cancel() wraps its DAO call in TransactionManager (a single
 * INSERT/UPDATE is already atomic), so — unlike AuthServiceImplTest — no MockedStatic is
 * needed here.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock private AppointmentDAO appointmentDAO;
    @Mock private PatientDAO     patientDAO;
    @Mock private DoctorDAO      doctorDAO;

    private AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AppointmentServiceImpl(appointmentDAO, patientDAO, doctorDAO);
    }

    private Appointment sampleAppointment(String id) {
        Appointment a = new Appointment();
        a.setAppointmentId(id);
        a.setPatientId(UUID.randomUUID().toString());
        a.setDoctorId(UUID.randomUUID().toString());
        a.setAppointmentDate(LocalDateTime.of(2026, 3, 15, 9, 30));
        a.setStatus("scheduled");
        a.setReason("Annual checkup");
        return a;
    }

    // ── book ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("book throws IllegalArgumentException when patientId is blank")
    void book_throwsIllegalArgumentException_whenPatientIdBlank() {
        CreateAppointmentDTO dto = new CreateAppointmentDTO("  ", UUID.randomUUID().toString(),
                LocalDateTime.now().plusDays(1), "Checkup");

        assertThrows(IllegalArgumentException.class, () -> service.book(dto));
        verifyNoInteractions(appointmentDAO);
    }

    @Test
    @DisplayName("book throws IllegalArgumentException when doctorId is blank")
    void book_throwsIllegalArgumentException_whenDoctorIdBlank() {
        CreateAppointmentDTO dto = new CreateAppointmentDTO(UUID.randomUUID().toString(), "  ",
                LocalDateTime.now().plusDays(1), "Checkup");

        assertThrows(IllegalArgumentException.class, () -> service.book(dto));
        verifyNoInteractions(appointmentDAO);
    }

    @Test
    @DisplayName("book throws ValidationException when appointmentDate is missing")
    void book_throwsValidationException_whenAppointmentDateMissing() {
        CreateAppointmentDTO dto = new CreateAppointmentDTO(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "Checkup");

        ValidationException ex = assertThrows(ValidationException.class, () -> service.book(dto));
        assertTrue(ex.getMessage().toLowerCase().contains("appointmentdate"));
    }

    @Test
    @DisplayName("book throws ValidationException when appointmentDate is in the past")
    void book_throwsValidationException_whenAppointmentDateInPast() {
        CreateAppointmentDTO dto = new CreateAppointmentDTO(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), LocalDateTime.now().minusDays(1), "Checkup");

        ValidationException ex = assertThrows(ValidationException.class, () -> service.book(dto));
        assertTrue(ex.getMessage().toLowerCase().contains("past"));
        verifyNoInteractions(appointmentDAO);
    }

    @Test
    @DisplayName("book saves a new appointment defaulted to 'scheduled' status when everything is valid")
    void book_savesAppointment_whenValid() throws Exception {
        String patientId = UUID.randomUUID().toString();
        String doctorId = UUID.randomUUID().toString();
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        CreateAppointmentDTO dto = new CreateAppointmentDTO(patientId, doctorId, date, "Checkup");
        when(appointmentDAO.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setAppointmentId(UUID.randomUUID().toString());
            return a;
        });

        AppointmentDTO result = service.book(dto);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentDAO).save(captor.capture());
        Appointment saved = captor.getValue();
        assertEquals(patientId, saved.getPatientId());
        assertEquals(doctorId, saved.getDoctorId());
        assertEquals("scheduled", saved.getStatus());
        assertEquals("scheduled", result.getStatus());
    }

    @Test
    @DisplayName("book translates a unique_violation (SQLSTATE 23505) into a friendly ValidationException "
            + "instead of letting the raw DatabaseException surface")
    void book_translatesUniqueViolation_toValidationException() throws Exception {
        CreateAppointmentDTO dto = new CreateAppointmentDTO(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), LocalDateTime.now().plusDays(30), "Checkup");
        SQLException uniqueViolation = new SQLException("duplicate key value violates unique constraint", "23505");
        when(appointmentDAO.save(any(Appointment.class)))
                .thenThrow(new DatabaseException("Failed to save appointment", uniqueViolation));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.book(dto));
        assertTrue(ex.getMessage().toLowerCase().contains("already has an appointment"));
    }

    @Test
    @DisplayName("book lets a non-constraint DatabaseException surface unchanged")
    void book_rethrowsNonUniqueViolationDatabaseException() throws Exception {
        CreateAppointmentDTO dto = new CreateAppointmentDTO(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), LocalDateTime.now().plusDays(30), "Checkup");
        DatabaseException connectionFailure = new DatabaseException("Could not obtain a database connection");
        when(appointmentDAO.save(any(Appointment.class))).thenThrow(connectionFailure);

        assertThrows(DatabaseException.class, () -> service.book(dto));
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns a mapped DTO when the DAO finds a matching appointment")
    void findById_returnsMappedDto_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(appointmentDAO.findById(id)).thenReturn(Optional.of(sampleAppointment(id)));

        AppointmentDTO dto = service.findById(id);

        assertEquals(id, dto.getAppointmentId());
        assertEquals("Annual checkup", dto.getReason());
        verify(appointmentDAO).findById(id);
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the DAO finds nothing")
    void findById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(appointmentDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll maps each appointment to a summary with resolved patient/doctor names")
    void findAll_resolvesNames() throws Exception {
        String patientId = UUID.randomUUID().toString();
        String doctorId = UUID.randomUUID().toString();
        Appointment appointment = sampleAppointment(UUID.randomUUID().toString());
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctorId);
        PageRequest request = CursorPagination.firstPage();
        when(appointmentDAO.findAll(request)).thenReturn(
                CursorPagination.toResult(List.of(appointment), request, Appointment::getCreatedAt));
        Patient patient = new Patient();
        patient.setFirstName("Jane");
        patient.setLastName("Doe");
        Doctor doctor = new Doctor();
        doctor.setFirstName("Greg");
        doctor.setLastName("House");
        when(patientDAO.findById(patientId)).thenReturn(Optional.of(patient));
        when(doctorDAO.findById(doctorId)).thenReturn(Optional.of(doctor));

        PageResult<AppointmentSummaryDTO> page = service.findAll(request);

        assertEquals(1, page.getCount());
        assertEquals("Jane Doe", page.getItems().get(0).getPatientName());
        assertEquals("Greg House", page.getItems().get(0).getDoctorName());
    }

    @Test
    @DisplayName("findAll falls back to the raw id when resolving the patient/doctor name throws")
    void findAll_fallsBackToRawId_whenResolutionFails() throws Exception {
        String patientId = UUID.randomUUID().toString();
        String doctorId = UUID.randomUUID().toString();
        Appointment appointment = sampleAppointment(UUID.randomUUID().toString());
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctorId);
        PageRequest request = CursorPagination.firstPage();
        when(appointmentDAO.findAll(request)).thenReturn(
                CursorPagination.toResult(List.of(appointment), request, Appointment::getCreatedAt));
        when(patientDAO.findById(patientId)).thenThrow(new RuntimeException("boom"));
        when(doctorDAO.findById(doctorId)).thenThrow(new RuntimeException("boom"));

        PageResult<AppointmentSummaryDTO> page = service.findAll(request);

        assertEquals(patientId, page.getItems().get(0).getPatientName());
        assertEquals(doctorId, page.getItems().get(0).getDoctorName());
    }

    // ── findByPatient / findByDoctor ──────────────────────────────────────

    @Test
    @DisplayName("findByPatient returns mapped DTOs for every appointment belonging to that patient")
    void findByPatient_returnsMappedDtos() throws Exception {
        String patientId = UUID.randomUUID().toString();
        Appointment appointment = sampleAppointment(UUID.randomUUID().toString());
        when(appointmentDAO.findByPatientId(patientId)).thenReturn(List.of(appointment));

        List<AppointmentDTO> result = service.findByPatient(patientId);

        assertEquals(1, result.size());
        assertEquals(appointment.getAppointmentId(), result.get(0).getAppointmentId());
    }

    @Test
    @DisplayName("findByDoctor returns mapped DTOs for every appointment belonging to that doctor")
    void findByDoctor_returnsMappedDtos() throws Exception {
        String doctorId = UUID.randomUUID().toString();
        Appointment appointment = sampleAppointment(UUID.randomUUID().toString());
        when(appointmentDAO.findByDoctorId(doctorId)).thenReturn(List.of(appointment));

        List<AppointmentDTO> result = service.findByDoctor(doctorId);

        assertEquals(1, result.size());
        assertEquals(appointment.getAppointmentId(), result.get(0).getAppointmentId());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update throws IllegalArgumentException when appointmentId is blank")
    void update_throwsIllegalArgumentException_whenAppointmentIdBlank() {
        UpdateAppointmentDTO dto = new UpdateAppointmentDTO("  ", null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.update(dto));
        verifyNoInteractions(appointmentDAO);
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException when the appointment doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(appointmentDAO.findById(id)).thenReturn(Optional.empty());
        UpdateAppointmentDTO dto = new UpdateAppointmentDTO(id, null, null, null);

        assertThrows(ResourceNotFoundException.class, () -> service.update(dto));
    }

    @Test
    @DisplayName("update throws ValidationException when the status is not a recognised AppointmentStatus")
    void update_throwsValidationException_whenStatusInvalid() throws Exception {
        String id = UUID.randomUUID().toString();
        when(appointmentDAO.findById(id)).thenReturn(Optional.of(sampleAppointment(id)));
        UpdateAppointmentDTO dto = new UpdateAppointmentDTO(id, null, "pending", null);

        assertThrows(ValidationException.class, () -> service.update(dto));
        verify(appointmentDAO, never()).update(any());
    }

    @Test
    @DisplayName("update applies only the fields provided, leaving the rest unchanged")
    void update_appliesOnlyProvidedFields() throws Exception {
        String id = UUID.randomUUID().toString();
        Appointment existing = sampleAppointment(id);
        when(appointmentDAO.findById(id)).thenReturn(Optional.of(existing));
        when(appointmentDAO.update(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateAppointmentDTO dto = new UpdateAppointmentDTO(id, null, "completed", null);

        AppointmentDTO result = service.update(dto);

        assertEquals("completed", result.getStatus());
        assertEquals("Annual checkup", result.getReason()); // unchanged, since dto.getReason() was null
    }

    // ── cancel ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancel throws ResourceNotFoundException when the appointment doesn't exist")
    void cancel_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(appointmentDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.cancel(id));
        verify(appointmentDAO, never()).softDelete(anyString());
    }

    @Test
    @DisplayName("cancel delegates to the DAO's soft-delete when the appointment exists")
    void cancel_delegatesToSoftDelete() throws Exception {
        String id = UUID.randomUUID().toString();
        when(appointmentDAO.findById(id)).thenReturn(Optional.of(sampleAppointment(id)));

        service.cancel(id);

        verify(appointmentDAO).softDelete(id);
    }
}
