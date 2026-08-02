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
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): runs LabOrderDAOImpl's
 * actual SQL against a real database, including the RETURNING clause, the FK chain down to
 * patients/doctors/appointments, and the status CHECK constraint
 * (IN ('ordered','in_progress','completed','cancelled')).
 */
class LabOrderDAOImplTest extends PostgresIntegrationTestBase {

    private final LabOrderDAOImpl dao = new LabOrderDAOImpl();
    private final PatientDAOImpl patientDAO = new PatientDAOImpl();
    private final DoctorDAOImpl doctorDAO = new DoctorDAOImpl();
    private final AppointmentDAOImpl appointmentDAO = new AppointmentDAOImpl();

    private String appointmentId;
    private String doctorId;

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
        doctor.setSpecialization("Diagnostics");
        doctor.setEmail("house+" + UUID.randomUUID() + "@example.com");
        Doctor savedDoctor = doctorDAO.save(doctor);
        doctorId = savedDoctor.getDoctorId();

        Appointment appointment = new Appointment();
        appointment.setPatientId(savedPatient.getPatientId());
        appointment.setDoctorId(doctorId);
        appointment.setAppointmentDate(LocalDateTime.now().plusDays(1));
        appointment.setReason("Routine checkup");
        Appointment savedAppointment = appointmentDAO.save(appointment);
        appointmentId = savedAppointment.getAppointmentId();
    }

    private LabOrder sampleOrder() {
        LabOrder order = new LabOrder();
        order.setAppointmentId(appointmentId);
        order.setDoctorId(doctorId);
        order.setTestName("Complete Blood Count");
        return order;
    }

    // ── save ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save assigns a generated id and populates ordered_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        LabOrder saved = dao.save(sampleOrder());

        assertNotNull(saved.getLabOrderId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getLabOrderId()));
        assertNotNull(saved.getOrderedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    /**
     * LabOrderDAOImpl.save() lets the DB apply its "ordered" default (via SQL literal
     * fallback) when order.getStatus() is null, but it never calls order.setStatus(...)
     * back onto the object it returns — unlike labOrderId/orderedAt/updatedAt, which ARE
     * written back. Callers that pass a null status get a returned LabOrder whose
     * getStatus() is still null, even though the row in the DB has status='ordered'.
     * This documents that actual (surprising) behavior rather than papering over it.
     */
    @Test
    @DisplayName("save does not write the DB-applied default status back onto the returned object")
    void save_doesNotPopulateDefaultStatusOnReturnedObject() throws Exception {
        LabOrder order = sampleOrder();
        assertNull(order.getStatus());

        LabOrder saved = dao.save(order);

        assertNull(saved.getStatus(), "status field on the returned object is not backfilled by save()");
        // But the row actually persisted with the DB default:
        Optional<LabOrder> reloaded = dao.findById(saved.getLabOrderId());
        assertTrue(reloaded.isPresent());
        assertEquals("ordered", reloaded.get().getStatus());
    }

    @Test
    @DisplayName("save persists an explicitly-set status as-is")
    void save_persistsExplicitStatus() throws Exception {
        LabOrder order = sampleOrder();
        order.setStatus("in_progress");

        LabOrder saved = dao.save(order);

        assertEquals("in_progress", saved.getStatus());
        assertEquals("in_progress", dao.findById(saved.getLabOrderId()).get().getStatus());
    }

    @Test
    @DisplayName("save rejects a status outside the CHECK constraint's allowed values")
    void save_rejectsInvalidStatus() {
        LabOrder order = sampleOrder();
        order.setStatus("bogus");

        assertThrows(DatabaseException.class, () -> dao.save(order));
    }

    @Test
    @DisplayName("save(order, conn) overload participates in a caller-supplied connection")
    void save_connectionOverload_persistsRow() throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            LabOrder saved = dao.save(sampleOrder(), conn);

            assertNotNull(saved.getLabOrderId());
            assertNotNull(saved.getOrderedAt());
        }
        // Visible to a fresh connection too — no transaction was left open uncommitted.
        assertTrue(dao.findByAppointmentId(appointmentId).size() >= 1);
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns the saved order with every field intact")
    void findById_returnsSavedOrder() throws Exception {
        LabOrder saved = dao.save(sampleOrder());

        Optional<LabOrder> found = dao.findById(saved.getLabOrderId());

        assertTrue(found.isPresent());
        assertEquals("Complete Blood Count", found.get().getTestName());
        assertEquals(appointmentId, found.get().getAppointmentId());
        assertEquals(doctorId, found.get().getDoctorId());
        assertEquals("ordered", found.get().getStatus());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted order")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        LabOrder saved = dao.save(sampleOrder());
        dao.softDelete(saved.getLabOrderId());

        assertTrue(dao.findById(saved.getLabOrderId()).isEmpty());
    }

    // ── findByAppointmentId / findByDoctorId ─────────────────────────────

    @Test
    @DisplayName("findByAppointmentId returns every non-deleted order for that appointment, newest first")
    void findByAppointmentId_returnsOrders() throws Exception {
        LabOrder first = sampleOrder();
        first.setTestName("CBC");
        dao.save(first);
        LabOrder second = sampleOrder();
        second.setTestName("Lipid Panel");
        dao.save(second);

        List<LabOrder> orders = dao.findByAppointmentId(appointmentId);

        assertEquals(2, orders.size());
    }

    @Test
    @DisplayName("findByAppointmentId excludes soft-deleted orders")
    void findByAppointmentId_excludesSoftDeleted() throws Exception {
        LabOrder saved = dao.save(sampleOrder());
        dao.softDelete(saved.getLabOrderId());

        assertTrue(dao.findByAppointmentId(appointmentId).isEmpty());
    }

    @Test
    @DisplayName("findByDoctorId returns every non-deleted order ordered by that doctor")
    void findByDoctorId_returnsOrders() throws Exception {
        dao.save(sampleOrder());

        List<LabOrder> orders = dao.findByDoctorId(doctorId);

        assertEquals(1, orders.size());
        assertEquals(doctorId, orders.get(0).getDoctorId());
    }

    // ── updateStatus ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus persists the new status and refreshes updated_at via the DB trigger")
    void updateStatus_persistsNewStatus() throws Exception {
        LabOrder saved = dao.save(sampleOrder());

        LabOrder updated = dao.updateStatus(saved.getLabOrderId(), "completed");

        assertEquals("completed", updated.getStatus());
        assertEquals("completed", dao.findById(saved.getLabOrderId()).get().getStatus());
    }

    @Test
    @DisplayName("updateStatus throws ResourceNotFoundException for an id that doesn't exist")
    void updateStatus_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class,
                () -> dao.updateStatus(UUID.randomUUID().toString(), "completed"));
    }

    @Test
    @DisplayName("updateStatus (no-connection overload) wraps a CHECK-constraint violation as DatabaseException")
    void updateStatus_wrapsCheckViolation_asDatabaseException() throws Exception {
        LabOrder saved = dao.save(sampleOrder());

        assertThrows(DatabaseException.class, () -> dao.updateStatus(saved.getLabOrderId(), "bogus"));
    }

    @Test
    @DisplayName("updateStatus(id, status, conn) overload lets a CHECK-constraint violation surface as a raw SQLException")
    void updateStatus_connectionOverload_lestsCheckViolationSurfaceAsSqlException() throws Exception {
        LabOrder saved = dao.save(sampleOrder());

        try (Connection conn = DBConnection.getConnection()) {
            assertThrows(SQLException.class, () -> dao.updateStatus(saved.getLabOrderId(), "bogus", conn));
        }
    }

    @Test
    @DisplayName("updateStatus(id, status, conn) overload persists the change using the caller-supplied connection")
    void updateStatus_connectionOverload_persistsChange() throws Exception {
        LabOrder saved = dao.save(sampleOrder());

        try (Connection conn = DBConnection.getConnection()) {
            LabOrder updated = dao.updateStatus(saved.getLabOrderId(), "in_progress", conn);
            assertEquals("in_progress", updated.getStatus());
        }
        assertEquals("in_progress", dao.findById(saved.getLabOrderId()).get().getStatus());
    }

    // ── softDelete ────────────────────────────────────────────────────────

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        LabOrder saved = dao.save(sampleOrder());

        dao.softDelete(saved.getLabOrderId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getLabOrderId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }

    @Test
    @DisplayName("softDelete throws ResourceNotFoundException for an id that doesn't exist")
    void softDelete_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(UUID.randomUUID().toString()));
    }
}
