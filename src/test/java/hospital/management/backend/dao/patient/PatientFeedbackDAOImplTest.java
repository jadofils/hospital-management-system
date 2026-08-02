package hospital.management.backend.dao.patient;

import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.model.patient.PatientFeedback;
import org.junit.jupiter.api.BeforeEach;
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
 * runs PatientFeedbackDAOImpl's actual SQL against a real database, proving the
 * COALESCE(?, CURRENT_DATE) default, the FK(patient_id)/FK(appointment_id) constraints,
 * and the rating CHECK constraint all behave as the code assumes.
 */
class PatientFeedbackDAOImplTest extends PostgresIntegrationTestBase {

    private final PatientFeedbackDAOImpl dao = new PatientFeedbackDAOImpl();
    private final PatientDAOImpl patientDAO = new PatientDAOImpl();
    private final DoctorDAOImpl doctorDAO = new DoctorDAOImpl();
    private final AppointmentDAOImpl appointmentDAO = new AppointmentDAOImpl();

    private String patientId;

    @BeforeEach
    void createParentPatient() throws Exception {
        Patient p = new Patient();
        p.setFirstName("Jane");
        p.setLastName("Doe");
        p.setDob(LocalDate.of(1990, 5, 20));
        p.setGender("F");
        p.setEmail("jane.doe." + UUID.randomUUID() + "@example.com");
        patientId = patientDAO.save(p).getPatientId();
    }

    private String createAppointmentForPatient(String forPatientId) throws Exception {
        Doctor d = new Doctor();
        d.setFirstName("Greg");
        d.setLastName("House");
        d.setEmail("dr.house." + UUID.randomUUID() + "@example.com");
        String doctorId = doctorDAO.save(d).getDoctorId();

        Appointment appt = new Appointment();
        appt.setPatientId(forPatientId);
        appt.setDoctorId(doctorId);
        appt.setAppointmentDate(LocalDateTime.now());
        appt.setReason("Checkup");
        return appointmentDAO.save(appt).getAppointmentId();
    }

    private PatientFeedback sampleFeedback(int rating) {
        PatientFeedback f = new PatientFeedback();
        f.setPatientId(patientId);
        f.setRating(rating);
        f.setComments("Great service");
        return f;
    }

    @Test
    @DisplayName("save assigns a generated id and lets the DB default date_submitted when none is supplied")
    void save_defaultsDateSubmitted_whenNoneSupplied() throws Exception {
        PatientFeedback saved = dao.save(sampleFeedback(5));

        assertNotNull(saved.getFeedbackId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getFeedbackId()));
        assertNotNull(saved.getDateSubmitted());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save persists an explicitly supplied date_submitted instead of the DB default")
    void save_persistsExplicitDateSubmitted() throws Exception {
        PatientFeedback feedback = sampleFeedback(4);
        LocalDate explicitDate = LocalDate.of(2020, 1, 15);
        feedback.setDateSubmitted(explicitDate);

        PatientFeedback saved = dao.save(feedback);

        assertEquals(explicitDate, saved.getDateSubmitted());
    }

    @Test
    @DisplayName("save persists an associated appointment_id when one is supplied")
    void save_persistsAppointmentId() throws Exception {
        String appointmentId = createAppointmentForPatient(patientId);
        PatientFeedback feedback = sampleFeedback(3);
        feedback.setAppointmentId(appointmentId);

        PatientFeedback saved = dao.save(feedback);

        Optional<PatientFeedback> found = dao.findById(saved.getFeedbackId());
        assertTrue(found.isPresent());
        assertEquals(appointmentId, found.get().getAppointmentId());
    }

    @Test
    @DisplayName("save allows a null appointment_id — the column is nullable")
    void save_allowsNullAppointmentId() throws Exception {
        PatientFeedback saved = dao.save(sampleFeedback(3));

        Optional<PatientFeedback> found = dao.findById(saved.getFeedbackId());
        assertTrue(found.isPresent());
        assertNull(found.get().getAppointmentId());
    }

    @Test
    @DisplayName("save rejects a rating outside the 1-5 CHECK constraint")
    void save_rejectsRatingOutOfRange() {
        assertThrows(DatabaseException.class, () -> dao.save(sampleFeedback(6)));
        assertThrows(DatabaseException.class, () -> dao.save(sampleFeedback(0)));
    }

    @Test
    @DisplayName("save rejects feedback referencing a patient_id that doesn't exist (FK violation)")
    void save_rejectsUnknownPatientId() {
        PatientFeedback orphan = sampleFeedback(5);
        orphan.setPatientId(UUID.randomUUID().toString());

        assertThrows(DatabaseException.class, () -> dao.save(orphan));
    }

    @Test
    @DisplayName("findById returns the saved feedback with every field intact")
    void findById_returnsSavedFeedback() throws Exception {
        PatientFeedback saved = dao.save(sampleFeedback(5));

        Optional<PatientFeedback> found = dao.findById(saved.getFeedbackId());

        assertTrue(found.isPresent());
        assertEquals(5, found.get().getRating());
        assertEquals("Great service", found.get().getComments());
        assertEquals(patientId, found.get().getPatientId());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted feedback entry")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        PatientFeedback saved = dao.save(sampleFeedback(5));
        dao.softDelete(saved.getFeedbackId());

        assertTrue(dao.findById(saved.getFeedbackId()).isEmpty());
    }

    @Test
    @DisplayName("findByPatientId returns only that patient's non-deleted feedback")
    void findByPatientId_returnsNonDeletedFeedbackForPatient() throws Exception {
        PatientFeedback kept = dao.save(sampleFeedback(5));
        PatientFeedback deleted = dao.save(sampleFeedback(1));
        dao.softDelete(deleted.getFeedbackId());

        List<PatientFeedback> found = dao.findByPatientId(patientId);

        assertEquals(1, found.size());
        assertEquals(kept.getFeedbackId(), found.get(0).getFeedbackId());
    }

    @Test
    @DisplayName("findByAppointmentId returns only feedback tied to that appointment")
    void findByAppointmentId_returnsMatchingFeedback() throws Exception {
        String appointmentId = createAppointmentForPatient(patientId);
        PatientFeedback withAppointment = sampleFeedback(4);
        withAppointment.setAppointmentId(appointmentId);
        dao.save(withAppointment);
        dao.save(sampleFeedback(2)); // no appointment — must not match

        List<PatientFeedback> found = dao.findByAppointmentId(appointmentId);

        assertEquals(1, found.size());
        assertEquals(appointmentId, found.get(0).getAppointmentId());
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it, and a second call throws")
    void softDelete_marksDeletedAt_andRejectsDoubleDelete() throws Exception {
        PatientFeedback saved = dao.save(sampleFeedback(5));

        dao.softDelete(saved.getFeedbackId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getFeedbackId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }

    @Test
    @DisplayName("softDelete throws ResourceNotFoundException for an id that was never saved")
    void softDelete_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class,
                () -> dao.softDelete(UUID.randomUUID().toString()));
    }
}
