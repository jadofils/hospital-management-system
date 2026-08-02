package hospital.management.backend.dao.clinical;

import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.MedicalRecord;
import hospital.management.backend.model.patient.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion here
 * runs MedicalRecordDAOImpl's actual SQL against a real database, proving the RETURNING
 * clauses, the appointment_id NOT NULL FK constraint, and — notably — the
 * UNIQUE(appointment_id) constraint that enforces the 1:0..1 relationship between an
 * appointment and its medical record. This is a real, DB-level uniqueness guarantee,
 * unlike patients.email (see PatientDAOImplTest.save_allowsDuplicateEmail_atDaoLevel),
 * so a second save() for the same appointment must fail here.
 */
class MedicalRecordDAOImplTest extends PostgresIntegrationTestBase {

    private final MedicalRecordDAOImpl dao           = new MedicalRecordDAOImpl();
    private final AppointmentDAOImpl   appointmentDAO = new AppointmentDAOImpl();
    private final PatientDAOImpl       patientDAO    = new PatientDAOImpl();
    private final DoctorDAOImpl        doctorDAO     = new DoctorDAOImpl();

    private Appointment savedAppointment() throws Exception {
        Patient patient = new Patient();
        patient.setFirstName("Jane");
        patient.setLastName("Doe");
        patient.setDob(LocalDate.of(1990, 5, 20));
        patient.setGender("F");
        patient.setEmail("jane.doe." + UUID.randomUUID() + "@example.com");
        Patient savedPatient = patientDAO.save(patient);

        Doctor doctor = new Doctor();
        doctor.setFirstName("Greg");
        doctor.setLastName("House");
        doctor.setSpecialization("Diagnostics");
        doctor.setEmail("greg.house." + UUID.randomUUID() + "@example.com");
        Doctor savedDoctor = doctorDAO.save(doctor);

        Appointment appointment = new Appointment();
        appointment.setPatientId(savedPatient.getPatientId());
        appointment.setDoctorId(savedDoctor.getDoctorId());
        appointment.setAppointmentDate(LocalDateTime.of(2026, 3, 15, 9, 30));
        appointment.setReason("Annual checkup");
        return appointmentDAO.save(appointment);
    }

    private MedicalRecord sampleRecord(String appointmentId) {
        MedicalRecord r = new MedicalRecord();
        r.setAppointmentId(appointmentId);
        r.setDiagnosis("Hypertension");
        r.setSymptoms("Headache, dizziness");
        r.setNotes("Prescribed lisinopril 10mg");
        return r;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        Appointment appointment = savedAppointment();

        MedicalRecord saved = dao.save(sampleRecord(appointment.getAppointmentId()));

        assertNotNull(saved.getRecordId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getRecordId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save rejects a second record for an appointment that already has one — appointment_id is UNIQUE")
    void save_throwsDatabaseException_whenAppointmentAlreadyHasRecord() throws Exception {
        Appointment appointment = savedAppointment();
        dao.save(sampleRecord(appointment.getAppointmentId()));

        assertThrows(DatabaseException.class, () -> dao.save(sampleRecord(appointment.getAppointmentId())));
    }

    @Test
    @DisplayName("save rejects an appointmentId that doesn't reference an existing appointment (FK violation)")
    void save_throwsDatabaseException_whenAppointmentMissing() {
        assertThrows(DatabaseException.class, () -> dao.save(sampleRecord(UUID.randomUUID().toString())));
    }

    @Test
    @DisplayName("findById returns the saved record with every field intact")
    void findById_returnsSavedRecord() throws Exception {
        Appointment appointment = savedAppointment();
        MedicalRecord saved = dao.save(sampleRecord(appointment.getAppointmentId()));

        Optional<MedicalRecord> found = dao.findById(saved.getRecordId());

        assertTrue(found.isPresent());
        assertEquals("Hypertension", found.get().getDiagnosis());
        assertEquals(appointment.getAppointmentId(), found.get().getAppointmentId());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted record")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        Appointment appointment = savedAppointment();
        MedicalRecord saved = dao.save(sampleRecord(appointment.getAppointmentId()));

        dao.softDelete(saved.getRecordId());

        assertTrue(dao.findById(saved.getRecordId()).isEmpty());
    }

    @Test
    @DisplayName("findByAppointmentId finds the record linked to that appointment")
    void findByAppointmentId_findsMatch() throws Exception {
        Appointment appointment = savedAppointment();
        dao.save(sampleRecord(appointment.getAppointmentId()));

        Optional<MedicalRecord> found = dao.findByAppointmentId(appointment.getAppointmentId());

        assertTrue(found.isPresent());
        assertEquals("Hypertension", found.get().getDiagnosis());
    }

    @Test
    @DisplayName("findByAppointmentId returns empty when no record exists for that appointment")
    void findByAppointmentId_returnsEmpty_whenNone() throws Exception {
        Appointment appointment = savedAppointment();

        assertTrue(dao.findByAppointmentId(appointment.getAppointmentId()).isEmpty());
    }

    @Test
    @DisplayName("update persists changed fields and refreshes updated_at via the DB trigger")
    void update_persistsChanges() throws Exception {
        Appointment appointment = savedAppointment();
        MedicalRecord saved = dao.save(sampleRecord(appointment.getAppointmentId()));

        saved.setDiagnosis("Type 2 Diabetes");
        saved.setNotes("Started on metformin");
        MedicalRecord updated = dao.update(saved);

        assertEquals("Type 2 Diabetes", updated.getDiagnosis());
        Optional<MedicalRecord> reloaded = dao.findById(saved.getRecordId());
        assertEquals("Type 2 Diabetes", reloaded.get().getDiagnosis());
        assertEquals("Started on metformin", reloaded.get().getNotes());
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException for a record id that doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() {
        MedicalRecord ghost = sampleRecord(UUID.randomUUID().toString());
        ghost.setRecordId(UUID.randomUUID().toString());

        assertThrows(ResourceNotFoundException.class, () -> dao.update(ghost));
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        Appointment appointment = savedAppointment();
        MedicalRecord saved = dao.save(sampleRecord(appointment.getAppointmentId()));

        dao.softDelete(saved.getRecordId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getRecordId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }
}
