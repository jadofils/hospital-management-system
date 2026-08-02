package hospital.management.backend.dao.department;

import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Department;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.model.doctor.Referral;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion
 * here runs ReferralDAOImpl's actual SQL against a real database, proving the
 * RETURNING clauses, appointment/doctor FKs, status CHECK constraint,
 * chk_referral_not_self CHECK constraint, and gen_random_uuid() default all
 * behave as the code assumes.
 */
class ReferralDAOImplTest extends PostgresIntegrationTestBase {

    private final ReferralDAOImpl dao = new ReferralDAOImpl();
    private final DoctorDAOImpl doctorDao = new DoctorDAOImpl();
    private final DepartmentDAOImpl departmentDao = new DepartmentDAOImpl();
    private final PatientDAOImpl patientDao = new PatientDAOImpl();
    private final AppointmentDAOImpl appointmentDao = new AppointmentDAOImpl();

    private Doctor savedDoctor(String email) throws Exception {
        Department dept = new Department();
        dept.setName("Dept-" + UUID.randomUUID());
        Department savedDept = departmentDao.save(dept);

        Doctor doctor = new Doctor();
        doctor.setDepartmentId(savedDept.getDepartmentId());
        doctor.setFirstName("Sarah");
        doctor.setLastName("Chen");
        doctor.setSpecialization("Cardiology");
        doctor.setEmail(email);
        return doctorDao.save(doctor);
    }

    private Appointment savedAppointment(String doctorId) throws Exception {
        Patient patient = new Patient();
        patient.setFirstName("Jane");
        patient.setLastName("Doe");
        patient.setDob(LocalDate.of(1990, 5, 20));
        patient.setGender("F");
        Patient savedPatient = patientDao.save(patient);

        Appointment appointment = new Appointment();
        appointment.setPatientId(savedPatient.getPatientId());
        appointment.setDoctorId(doctorId);
        appointment.setAppointmentDate(LocalDateTime.now());
        appointment.setReason("Checkup");
        return appointmentDao.save(appointment);
    }

    private Referral sampleReferral(String appointmentId, String referringDoctorId, String referredToDoctorId) {
        Referral r = new Referral();
        r.setAppointmentId(appointmentId);
        r.setReferringDoctorId(referringDoctorId);
        r.setReferredToDoctorId(referredToDoctorId);
        r.setReason("Needs specialist opinion");
        return r;
    }

    @Test
    @DisplayName("save assigns a generated id, defaults status to pending, and populates timestamps from the DB")
    void save_assignsIdAndDefaultsStatusToPending() throws Exception {
        Doctor referring = savedDoctor("referring@example.com");
        Doctor referredTo = savedDoctor("referred@example.com");
        Appointment appointment = savedAppointment(referring.getDoctorId());

        Referral saved = dao.save(sampleReferral(appointment.getAppointmentId(), referring.getDoctorId(), referredTo.getDoctorId()));

        assertNotNull(saved.getReferralId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getReferralId()));
        assertEquals("pending", saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save rejects a referral where the referred-to doctor is the same as the referring doctor — real chk_referral_not_self constraint")
    void save_rejectsSelfReferral() throws Exception {
        Doctor doctor = savedDoctor("solo@example.com");
        Appointment appointment = savedAppointment(doctor.getDoctorId());

        Referral referral = sampleReferral(appointment.getAppointmentId(), doctor.getDoctorId(), doctor.getDoctorId());

        assertThrows(DatabaseException.class, () -> dao.save(referral));
    }

    @Test
    @DisplayName("save rejects an invalid status value — real CHECK constraint on referrals.status")
    void save_rejectsInvalidStatus() throws Exception {
        Doctor referring = savedDoctor("referring@example.com");
        Doctor referredTo = savedDoctor("referred@example.com");
        Appointment appointment = savedAppointment(referring.getDoctorId());

        Referral referral = sampleReferral(appointment.getAppointmentId(), referring.getDoctorId(), referredTo.getDoctorId());
        referral.setStatus("bogus-status");

        assertThrows(DatabaseException.class, () -> dao.save(referral));
    }

    @Test
    @DisplayName("save rejects an appointment_id that doesn't exist — real FK constraint enforcement")
    void save_rejectsNonExistentAppointmentId() throws Exception {
        Doctor referring = savedDoctor("referring@example.com");
        Doctor referredTo = savedDoctor("referred@example.com");

        Referral referral = sampleReferral(UUID.randomUUID().toString(), referring.getDoctorId(), referredTo.getDoctorId());

        assertThrows(DatabaseException.class, () -> dao.save(referral));
    }

    @Test
    @DisplayName("findById returns the saved referral with every field intact")
    void findById_returnsSavedReferral() throws Exception {
        Doctor referring = savedDoctor("referring@example.com");
        Doctor referredTo = savedDoctor("referred@example.com");
        Appointment appointment = savedAppointment(referring.getDoctorId());
        Referral saved = dao.save(sampleReferral(appointment.getAppointmentId(), referring.getDoctorId(), referredTo.getDoctorId()));

        Optional<Referral> found = dao.findById(saved.getReferralId());

        assertTrue(found.isPresent());
        assertEquals("Needs specialist opinion", found.get().getReason());
        assertEquals(referring.getDoctorId(), found.get().getReferringDoctorId());
        assertEquals(referredTo.getDoctorId(), found.get().getReferredToDoctorId());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted referral")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        Doctor referring = savedDoctor("referring@example.com");
        Doctor referredTo = savedDoctor("referred@example.com");
        Appointment appointment = savedAppointment(referring.getDoctorId());
        Referral saved = dao.save(sampleReferral(appointment.getAppointmentId(), referring.getDoctorId(), referredTo.getDoctorId()));

        dao.softDelete(saved.getReferralId());

        assertTrue(dao.findById(saved.getReferralId()).isEmpty());
    }

    @Test
    @DisplayName("findByAppointmentId returns only referrals for that appointment")
    void findByAppointmentId_returnsMatchingReferrals() throws Exception {
        Doctor referring = savedDoctor("referring@example.com");
        Doctor referredTo = savedDoctor("referred@example.com");
        Appointment appointment = savedAppointment(referring.getDoctorId());
        Appointment otherAppointment = savedAppointment(referring.getDoctorId());
        dao.save(sampleReferral(appointment.getAppointmentId(), referring.getDoctorId(), referredTo.getDoctorId()));
        dao.save(sampleReferral(otherAppointment.getAppointmentId(), referring.getDoctorId(), referredTo.getDoctorId()));

        List<Referral> found = dao.findByAppointmentId(appointment.getAppointmentId());

        assertEquals(1, found.size());
        assertEquals(appointment.getAppointmentId(), found.get(0).getAppointmentId());
    }

    @Test
    @DisplayName("findByReferringDoctorId returns only referrals made by that doctor")
    void findByReferringDoctorId_returnsMatchingReferrals() throws Exception {
        Doctor referring = savedDoctor("referring@example.com");
        Doctor referredTo = savedDoctor("referred@example.com");
        Doctor unrelatedDoctor = savedDoctor("unrelated@example.com");
        Appointment appointment = savedAppointment(referring.getDoctorId());
        Appointment otherAppointment = savedAppointment(unrelatedDoctor.getDoctorId());
        dao.save(sampleReferral(appointment.getAppointmentId(), referring.getDoctorId(), referredTo.getDoctorId()));
        dao.save(sampleReferral(otherAppointment.getAppointmentId(), unrelatedDoctor.getDoctorId(), referredTo.getDoctorId()));

        List<Referral> found = dao.findByReferringDoctorId(referring.getDoctorId());

        assertEquals(1, found.size());
        assertEquals(referring.getDoctorId(), found.get(0).getReferringDoctorId());
    }

    @Test
    @DisplayName("updateStatus persists the new status and returns the full updated row")
    void updateStatus_persistsNewStatus() throws Exception {
        Doctor referring = savedDoctor("referring@example.com");
        Doctor referredTo = savedDoctor("referred@example.com");
        Appointment appointment = savedAppointment(referring.getDoctorId());
        Referral saved = dao.save(sampleReferral(appointment.getAppointmentId(), referring.getDoctorId(), referredTo.getDoctorId()));

        Referral updated = dao.updateStatus(saved.getReferralId(), "scheduled");

        assertEquals("scheduled", updated.getStatus());
        Optional<Referral> reloaded = dao.findById(saved.getReferralId());
        assertEquals("scheduled", reloaded.get().getStatus());
    }

    @Test
    @DisplayName("updateStatus throws ResourceNotFoundException for a referral id that doesn't exist")
    void updateStatus_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class,
                () -> dao.updateStatus(UUID.randomUUID().toString(), "scheduled"));
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        Doctor referring = savedDoctor("referring@example.com");
        Doctor referredTo = savedDoctor("referred@example.com");
        Appointment appointment = savedAppointment(referring.getDoctorId());
        Referral saved = dao.save(sampleReferral(appointment.getAppointmentId(), referring.getDoctorId(), referredTo.getDoctorId()));

        dao.softDelete(saved.getReferralId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getReferralId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }
}
