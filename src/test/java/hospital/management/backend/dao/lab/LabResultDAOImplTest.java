package hospital.management.backend.dao.lab;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.model.lab.LabOrder;
import hospital.management.backend.model.lab.LabResult;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): runs LabResultDAOImpl's
 * actual SQL against a real database, including the 1:1 UNIQUE(lab_order_id) FK to lab_orders
 * (ON DELETE CASCADE) and the is_abnormal NOT NULL DEFAULT FALSE column.
 */
class LabResultDAOImplTest extends PostgresIntegrationTestBase {

    private final LabResultDAOImpl dao = new LabResultDAOImpl();
    private final LabOrderDAOImpl labOrderDAO = new LabOrderDAOImpl();
    private final PatientDAOImpl patientDAO = new PatientDAOImpl();
    private final DoctorDAOImpl doctorDAO = new DoctorDAOImpl();
    private final AppointmentDAOImpl appointmentDAO = new AppointmentDAOImpl();

    private String labOrderId;

    @BeforeEach
    void seedFixtures() throws Exception {
        Patient patient = new Patient();
        patient.setFirstName("Jane");
        patient.setLastName("Doe");
        patient.setDob(LocalDate.of(1990, 5, 20));
        patient.setGender("F");
        patient.setEmail("jane.doe@example.com");
        Patient savedPatient = patientDAO.save(patient);

        Doctor doctor = new Doctor();
        doctor.setFirstName("Greg");
        doctor.setLastName("House");
        doctor.setEmail("house+" + UUID.randomUUID() + "@example.com");
        Doctor savedDoctor = doctorDAO.save(doctor);

        Appointment appointment = new Appointment();
        appointment.setPatientId(savedPatient.getPatientId());
        appointment.setDoctorId(savedDoctor.getDoctorId());
        appointment.setAppointmentDate(LocalDateTime.now().plusDays(1));
        Appointment savedAppointment = appointmentDAO.save(appointment);

        LabOrder order = new LabOrder();
        order.setAppointmentId(savedAppointment.getAppointmentId());
        order.setDoctorId(savedDoctor.getDoctorId());
        order.setTestName("Complete Blood Count");
        LabOrder savedOrder = labOrderDAO.save(order);
        labOrderId = savedOrder.getLabOrderId();
    }

    private LabResult sampleResult() {
        LabResult result = new LabResult();
        result.setLabOrderId(labOrderId);
        result.setResultValue("5.4");
        result.setUnit("x10^9/L");
        result.setReferenceRange("4.0-11.0");
        result.setIsAbnormal(false);
        return result;
    }

    // ── save ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        LabResult saved = dao.save(sampleResult());

        assertNotNull(saved.getLabResultId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getLabResultId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save persists a null completed_at as NULL rather than throwing")
    void save_allowsNullCompletedAt() throws Exception {
        LabResult result = sampleResult();
        assertNull(result.getCompletedAt());

        LabResult saved = dao.save(result);

        assertNull(dao.findById(saved.getLabResultId()).get().getCompletedAt());
    }

    @Test
    @DisplayName("save persists a non-null completed_at")
    void save_persistsCompletedAt() throws Exception {
        LabResult result = sampleResult();
        LocalDateTime completedAt = LocalDateTime.now().minusHours(1).withNano(0);
        result.setCompletedAt(completedAt);

        LabResult saved = dao.save(result);

        assertEquals(completedAt, dao.findById(saved.getLabResultId()).get().getCompletedAt());
    }

    /**
     * lab_results.lab_order_id is UNIQUE — the DB itself enforces the 1:1 relationship
     * with lab_orders that LabServiceImpl.recordResult() also checks for (findByLabOrderId
     * before insert). Unlike the patients.email gap, this is a real DB-level guarantee:
     * a second save() for the same lab_order_id fails at the DAO level too.
     */
    @Test
    @DisplayName("save rejects a second result for the same lab order — UNIQUE(lab_order_id) constraint")
    void save_rejectsDuplicateLabOrderId() throws Exception {
        dao.save(sampleResult());

        assertThrows(DatabaseException.class, () -> dao.save(sampleResult()));
    }

    @Test
    @DisplayName("save(result, conn) overload participates in a caller-supplied connection")
    void save_connectionOverload_persistsRow() throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            LabResult saved = dao.save(sampleResult(), conn);
            assertNotNull(saved.getLabResultId());
        }
        assertTrue(dao.findByLabOrderId(labOrderId).isPresent());
    }

    @Test
    @DisplayName("save(result, conn) overload lets a UNIQUE-constraint violation surface as a raw SQLException")
    void save_connectionOverload_letsUniqueViolationSurfaceAsSqlException() throws Exception {
        dao.save(sampleResult());

        try (Connection conn = DBConnection.getConnection()) {
            assertThrows(SQLException.class, () -> dao.save(sampleResult(), conn));
        }
    }

    // ── findById / findByLabOrderId ───────────────────────────────────────

    @Test
    @DisplayName("findById returns the saved result with every field intact")
    void findById_returnsSavedResult() throws Exception {
        LabResult saved = dao.save(sampleResult());

        Optional<LabResult> found = dao.findById(saved.getLabResultId());

        assertTrue(found.isPresent());
        assertEquals("5.4", found.get().getResultValue());
        assertEquals("x10^9/L", found.get().getUnit());
        assertEquals("4.0-11.0", found.get().getReferenceRange());
        assertFalse(found.get().isIsAbnormal());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted result")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        LabResult saved = dao.save(sampleResult());
        dao.softDelete(saved.getLabResultId());

        assertTrue(dao.findById(saved.getLabResultId()).isEmpty());
    }

    @Test
    @DisplayName("findByLabOrderId finds the 1:1 result for its parent order")
    void findByLabOrderId_findsMatch() throws Exception {
        dao.save(sampleResult());

        Optional<LabResult> found = dao.findByLabOrderId(labOrderId);

        assertTrue(found.isPresent());
        assertEquals(labOrderId, found.get().getLabOrderId());
    }

    @Test
    @DisplayName("findByLabOrderId returns empty when no result has been recorded yet")
    void findByLabOrderId_returnsEmpty_whenNoneRecorded() throws Exception {
        assertTrue(dao.findByLabOrderId(labOrderId).isEmpty());
    }

    // ── softDelete ────────────────────────────────────────────────────────

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        LabResult saved = dao.save(sampleResult());

        dao.softDelete(saved.getLabResultId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getLabResultId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }

    @Test
    @DisplayName("softDelete throws ResourceNotFoundException for an id that doesn't exist")
    void softDelete_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(UUID.randomUUID().toString()));
    }
}
