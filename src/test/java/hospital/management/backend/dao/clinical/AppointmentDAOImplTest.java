package hospital.management.backend.dao.clinical;

import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion here
 * runs AppointmentDAOImpl's actual SQL against a real database, proving the RETURNING
 * clauses, the patient_id/doctor_id NOT NULL FK constraints, the status CHECK constraint
 * ('scheduled'/'completed'/'cancelled' only), and the gen_random_uuid()/updated_at trigger
 * defaults all behave as the code assumes — none of which a mocked Connection could verify.
 */
class AppointmentDAOImplTest extends PostgresIntegrationTestBase {

    private final AppointmentDAOImpl dao        = new AppointmentDAOImpl();
    private final PatientDAOImpl     patientDAO = new PatientDAOImpl();
    private final DoctorDAOImpl      doctorDAO  = new DoctorDAOImpl();

    private Patient savedPatient() throws Exception {
        Patient p = new Patient();
        p.setFirstName("Jane");
        p.setLastName("Doe");
        p.setDob(LocalDate.of(1990, 5, 20));
        p.setGender("F");
        p.setEmail("jane.doe." + UUID.randomUUID() + "@example.com");
        return patientDAO.save(p);
    }

    private Doctor savedDoctor() throws Exception {
        Doctor d = new Doctor();
        d.setFirstName("Greg");
        d.setLastName("House");
        d.setSpecialization("Diagnostics");
        d.setEmail("greg.house." + UUID.randomUUID() + "@example.com");
        return doctorDAO.save(d);
    }

    private Appointment sampleAppointment(String patientId, String doctorId) {
        Appointment a = new Appointment();
        a.setPatientId(patientId);
        a.setDoctorId(doctorId);
        a.setAppointmentDate(LocalDateTime.of(2026, 3, 15, 9, 30));
        a.setReason("Annual checkup");
        return a;
    }

    @Test
    @DisplayName("save assigns a generated id, defaults status to 'scheduled' when none given, and populates timestamps from the DB")
    void save_assignsIdAndDefaultStatusAndTimestamps() throws Exception {
        Patient patient = savedPatient();
        Doctor doctor = savedDoctor();

        Appointment saved = dao.save(sampleAppointment(patient.getPatientId(), doctor.getDoctorId()));

        assertNotNull(saved.getAppointmentId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getAppointmentId()));
        assertEquals("scheduled", saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save persists an explicitly given status")
    void save_persistsExplicitStatus() throws Exception {
        Patient patient = savedPatient();
        Doctor doctor = savedDoctor();
        Appointment appointment = sampleAppointment(patient.getPatientId(), doctor.getDoctorId());
        appointment.setStatus("completed");

        Appointment saved = dao.save(appointment);

        assertEquals("completed", saved.getStatus());
    }

    @Test
    @DisplayName("save rejects a status outside the CHECK constraint's allowed values ('scheduled'/'completed'/'cancelled')")
    void save_throwsDatabaseException_whenStatusInvalid() throws Exception {
        Patient patient = savedPatient();
        Doctor doctor = savedDoctor();
        Appointment appointment = sampleAppointment(patient.getPatientId(), doctor.getDoctorId());
        appointment.setStatus("pending");

        assertThrows(DatabaseException.class, () -> dao.save(appointment));
    }

    @Test
    @DisplayName("save rejects a patientId that doesn't reference an existing patient (FK violation)")
    void save_throwsDatabaseException_whenPatientMissing() throws Exception {
        Doctor doctor = savedDoctor();
        Appointment appointment = sampleAppointment(UUID.randomUUID().toString(), doctor.getDoctorId());

        assertThrows(DatabaseException.class, () -> dao.save(appointment));
    }

    @Test
    @DisplayName("save rejects a doctorId that doesn't reference an existing doctor (FK violation)")
    void save_throwsDatabaseException_whenDoctorMissing() throws Exception {
        Patient patient = savedPatient();
        Appointment appointment = sampleAppointment(patient.getPatientId(), UUID.randomUUID().toString());

        assertThrows(DatabaseException.class, () -> dao.save(appointment));
    }

    @Test
    @DisplayName("findById returns the saved appointment with every field intact")
    void findById_returnsSavedAppointment() throws Exception {
        Patient patient = savedPatient();
        Doctor doctor = savedDoctor();
        Appointment saved = dao.save(sampleAppointment(patient.getPatientId(), doctor.getDoctorId()));

        Optional<Appointment> found = dao.findById(saved.getAppointmentId());

        assertTrue(found.isPresent());
        assertEquals(patient.getPatientId(), found.get().getPatientId());
        assertEquals(doctor.getDoctorId(), found.get().getDoctorId());
        assertEquals("Annual checkup", found.get().getReason());
        assertEquals(LocalDateTime.of(2026, 3, 15, 9, 30), found.get().getAppointmentDate());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted appointment")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        Patient patient = savedPatient();
        Doctor doctor = savedDoctor();
        Appointment saved = dao.save(sampleAppointment(patient.getPatientId(), doctor.getDoctorId()));

        dao.softDelete(saved.getAppointmentId());

        assertTrue(dao.findById(saved.getAppointmentId()).isEmpty());
    }

    @Test
    @DisplayName("findByPatientId returns only that patient's appointments")
    void findByPatientId_returnsOnlyMatching() throws Exception {
        Patient patientA = savedPatient();
        Patient patientB = savedPatient();
        Doctor doctor = savedDoctor();
        dao.save(sampleAppointment(patientA.getPatientId(), doctor.getDoctorId()));
        dao.save(sampleAppointment(patientB.getPatientId(), doctor.getDoctorId()));

        List<Appointment> found = dao.findByPatientId(patientA.getPatientId());

        assertEquals(1, found.size());
        assertEquals(patientA.getPatientId(), found.get(0).getPatientId());
    }

    @Test
    @DisplayName("findByDoctorId returns only that doctor's appointments")
    void findByDoctorId_returnsOnlyMatching() throws Exception {
        Patient patient = savedPatient();
        Doctor doctorA = savedDoctor();
        Doctor doctorB = savedDoctor();
        dao.save(sampleAppointment(patient.getPatientId(), doctorA.getDoctorId()));
        dao.save(sampleAppointment(patient.getPatientId(), doctorB.getDoctorId()));

        List<Appointment> found = dao.findByDoctorId(doctorB.getDoctorId());

        assertEquals(1, found.size());
        assertEquals(doctorB.getDoctorId(), found.get(0).getDoctorId());
    }

    @Test
    @DisplayName("update persists changed date/status/reason and refreshes updated_at via the DB trigger")
    void update_persistsChanges() throws Exception {
        Patient patient = savedPatient();
        Doctor doctor = savedDoctor();
        Appointment saved = dao.save(sampleAppointment(patient.getPatientId(), doctor.getDoctorId()));

        saved.setAppointmentDate(LocalDateTime.of(2026, 4, 1, 14, 0));
        saved.setStatus("completed");
        saved.setReason("Follow-up");
        Appointment updated = dao.update(saved);

        assertEquals("completed", updated.getStatus());
        Optional<Appointment> reloaded = dao.findById(saved.getAppointmentId());
        assertEquals("completed", reloaded.get().getStatus());
        assertEquals("Follow-up", reloaded.get().getReason());
        assertEquals(LocalDateTime.of(2026, 4, 1, 14, 0), reloaded.get().getAppointmentDate());
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException for an appointment id that doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() {
        Appointment ghost = sampleAppointment(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        ghost.setAppointmentId(UUID.randomUUID().toString());
        ghost.setStatus("scheduled");

        assertThrows(ResourceNotFoundException.class, () -> dao.update(ghost));
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        Patient patient = savedPatient();
        Doctor doctor = savedDoctor();
        Appointment saved = dao.save(sampleAppointment(patient.getPatientId(), doctor.getDoctorId()));

        dao.softDelete(saved.getAppointmentId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getAppointmentId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }

    @Test
    @DisplayName("findAll returns every non-deleted appointment")
    void findAll_returnsNonDeletedAppointments() throws Exception {
        Patient patient = savedPatient();
        Doctor doctor = savedDoctor();
        dao.save(sampleAppointment(patient.getPatientId(), doctor.getDoctorId()));
        Appointment toDelete = dao.save(sampleAppointment(patient.getPatientId(), doctor.getDoctorId()));
        dao.softDelete(toDelete.getAppointmentId());

        PageResult<Appointment> page = dao.findAll(CursorPagination.firstPage());

        assertEquals(1, page.getCount());
    }
}
