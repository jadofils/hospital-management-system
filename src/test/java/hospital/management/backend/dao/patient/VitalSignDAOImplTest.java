package hospital.management.backend.dao.patient;

import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.model.patient.VitalSign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion here
 * runs VitalSignDAOImpl's actual SQL against a real database, proving the FK(appointment_id)
 * constraint, the blood-pressure/heart-rate CHECK constraints, and the join through
 * `appointments` used by findByPatientId all behave as the code assumes.
 *
 * vital_signs has no patient_id column of its own — it is scoped to an appointment, which is
 * scoped to a patient (see VitalSignDAOImpl javadoc) — so every fixture here creates a
 * patient + doctor + appointment chain before exercising the DAO.
 */
class VitalSignDAOImplTest extends PostgresIntegrationTestBase {

    private final VitalSignDAOImpl dao = new VitalSignDAOImpl();
    private final PatientDAOImpl patientDAO = new PatientDAOImpl();
    private final DoctorDAOImpl doctorDAO = new DoctorDAOImpl();
    private final AppointmentDAOImpl appointmentDAO = new AppointmentDAOImpl();

    private String patientId;
    private String appointmentId;

    @BeforeEach
    void createParentPatientAndAppointment() throws Exception {
        Patient p = new Patient();
        p.setFirstName("Jane");
        p.setLastName("Doe");
        p.setDob(LocalDate.of(1990, 5, 20));
        p.setGender("F");
        p.setEmail("jane.doe." + UUID.randomUUID() + "@example.com");
        patientId = patientDAO.save(p).getPatientId();

        appointmentId = createAppointmentForPatient(patientId);
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

    private VitalSign sampleVitalSign() {
        VitalSign v = new VitalSign();
        v.setAppointmentId(appointmentId);
        v.setBloodPressureSystolic(120);
        v.setBloodPressureDiastolic(80);
        v.setHeartRate(72);
        v.setTemperatureCelsius(new BigDecimal("36.6"));
        v.setWeightKg(new BigDecimal("70.50"));
        v.setHeightCm(new BigDecimal("175.00"));
        return v;
    }

    @Test
    @DisplayName("save assigns a generated id and populates recorded_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        VitalSign saved = dao.save(sampleVitalSign());

        assertNotNull(saved.getVitalId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getVitalId()));
        assertNotNull(saved.getRecordedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save allows every measurement column to be null — only appointment_id is required")
    void save_allowsAllMeasurementsNull() throws Exception {
        VitalSign v = new VitalSign();
        v.setAppointmentId(appointmentId);

        VitalSign saved = dao.save(v);

        Optional<VitalSign> found = dao.findById(saved.getVitalId());
        assertTrue(found.isPresent());
        assertNull(found.get().getBloodPressureSystolic());
        assertNull(found.get().getBloodPressureDiastolic());
        assertNull(found.get().getHeartRate());
        assertNull(found.get().getTemperatureCelsius());
        assertNull(found.get().getWeightKg());
        assertNull(found.get().getHeightCm());
    }

    @Test
    @DisplayName("save rejects a systolic blood pressure outside the 1-300 CHECK constraint")
    void save_rejectsSystolicOutOfRange() {
        VitalSign v = sampleVitalSign();
        v.setBloodPressureSystolic(301);

        assertThrows(DatabaseException.class, () -> dao.save(v));
    }

    @Test
    @DisplayName("save rejects a diastolic blood pressure outside the 1-200 CHECK constraint")
    void save_rejectsDiastolicOutOfRange() {
        VitalSign v = sampleVitalSign();
        v.setBloodPressureDiastolic(201);

        assertThrows(DatabaseException.class, () -> dao.save(v));
    }

    @Test
    @DisplayName("save rejects a non-positive heart rate")
    void save_rejectsNonPositiveHeartRate() {
        VitalSign v = sampleVitalSign();
        v.setHeartRate(0);

        assertThrows(DatabaseException.class, () -> dao.save(v));
    }

    @Test
    @DisplayName("save rejects a vital sign referencing an appointment_id that doesn't exist (FK violation)")
    void save_rejectsUnknownAppointmentId() {
        VitalSign orphan = sampleVitalSign();
        orphan.setAppointmentId(UUID.randomUUID().toString());

        assertThrows(DatabaseException.class, () -> dao.save(orphan));
    }

    @Test
    @DisplayName("findById returns the saved vital sign with every field intact")
    void findById_returnsSavedVitalSign() throws Exception {
        VitalSign saved = dao.save(sampleVitalSign());

        Optional<VitalSign> found = dao.findById(saved.getVitalId());

        assertTrue(found.isPresent());
        assertEquals(120, found.get().getBloodPressureSystolic());
        assertEquals(80, found.get().getBloodPressureDiastolic());
        assertEquals(72, found.get().getHeartRate());
        assertEquals(0, new BigDecimal("36.6").compareTo(found.get().getTemperatureCelsius()));
        assertEquals(appointmentId, found.get().getAppointmentId());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted vital sign")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        VitalSign saved = dao.save(sampleVitalSign());
        dao.softDelete(saved.getVitalId());

        assertTrue(dao.findById(saved.getVitalId()).isEmpty());
    }

    @Test
    @DisplayName("findByAppointmentId returns the vital sign recorded for that appointment")
    void findByAppointmentId_returnsMatch() throws Exception {
        VitalSign saved = dao.save(sampleVitalSign());

        Optional<VitalSign> found = dao.findByAppointmentId(appointmentId);

        assertTrue(found.isPresent());
        assertEquals(saved.getVitalId(), found.get().getVitalId());
    }

    @Test
    @DisplayName("findByAppointmentId returns empty when no vital sign was recorded for that appointment")
    void findByAppointmentId_returnsEmpty_whenNoneRecorded() throws Exception {
        assertTrue(dao.findByAppointmentId(appointmentId).isEmpty());
    }

    @Test
    @DisplayName("findByPatientId joins through appointments and returns vitals for that patient only")
    void findByPatientId_joinsThroughAppointments() throws Exception {
        VitalSign saved = dao.save(sampleVitalSign());

        // A second, unrelated patient's vital sign (own appointment) must not leak in.
        Patient other = new Patient();
        other.setFirstName("Bob");
        other.setLastName("Smith");
        other.setDob(LocalDate.of(1985, 1, 1));
        other.setGender("M");
        String otherPatientId = patientDAO.save(other).getPatientId();
        String otherAppointmentId = createAppointmentForPatient(otherPatientId);
        VitalSign otherVital = sampleVitalSign();
        otherVital.setAppointmentId(otherAppointmentId);
        dao.save(otherVital);

        List<VitalSign> found = dao.findByPatientId(patientId);

        assertEquals(1, found.size());
        assertEquals(saved.getVitalId(), found.get(0).getVitalId());
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it, and a second call throws")
    void softDelete_marksDeletedAt_andRejectsDoubleDelete() throws Exception {
        VitalSign saved = dao.save(sampleVitalSign());

        dao.softDelete(saved.getVitalId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getVitalId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }

    @Test
    @DisplayName("softDelete throws ResourceNotFoundException for an id that was never saved")
    void softDelete_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class,
                () -> dao.softDelete(UUID.randomUUID().toString()));
    }
}
