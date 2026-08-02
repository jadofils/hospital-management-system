package hospital.management.backend.dao.finance;

import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageResult;
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
 * Real Postgres integration test (see PostgresIntegrationTestBase): runs InvoiceDAOImpl's
 * actual SQL against a real database, including the FK chain down to patients/appointments,
 * the total_amount >= 0 CHECK, and the payment_status CHECK IN ('unpaid','partially_paid','paid').
 *
 * Note: Invoice (backend/model/finance) uses JavaFX SimpleXxxProperty fields rather than plain
 * getter/setter fields like Patient/Doctor/Appointment — its public getter/setter API is
 * identical in shape though, so fixtures below are built the same way as other DAO tests.
 */
class InvoiceDAOImplTest extends PostgresIntegrationTestBase {

    private final InvoiceDAOImpl dao = new InvoiceDAOImpl();
    private final PatientDAOImpl patientDAO = new PatientDAOImpl();
    private final DoctorDAOImpl doctorDAO = new DoctorDAOImpl();
    private final AppointmentDAOImpl appointmentDAO = new AppointmentDAOImpl();

    private String patientId;
    private String appointmentId;

    @BeforeEach
    void seedFixtures() throws Exception {
        Patient patient = new Patient();
        patient.setFirstName("Jane");
        patient.setLastName("Doe");
        patient.setDob(LocalDate.of(1990, 5, 20));
        patient.setGender("F");
        patient.setEmail("jane.doe@example.com");
        Patient savedPatient = patientDAO.save(patient);
        patientId = savedPatient.getPatientId();

        Doctor doctor = new Doctor();
        doctor.setFirstName("Greg");
        doctor.setLastName("House");
        doctor.setEmail("house+" + UUID.randomUUID() + "@example.com");
        Doctor savedDoctor = doctorDAO.save(doctor);

        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(savedDoctor.getDoctorId());
        appointment.setAppointmentDate(LocalDateTime.now().plusDays(1));
        Appointment savedAppointment = appointmentDAO.save(appointment);
        appointmentId = savedAppointment.getAppointmentId();
    }

    private Invoice sampleInvoice() {
        Invoice invoice = new Invoice();
        invoice.setAppointmentId(appointmentId);
        invoice.setPatientId(patientId);
        invoice.setTotalAmount(new BigDecimal("150.00"));
        return invoice;
    }

    // ── save ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save assigns a generated id and populates issued_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        Invoice saved = dao.save(sampleInvoice());

        assertNotNull(saved.getInvoiceId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getInvoiceId()));
        assertNotNull(saved.getIssuedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save defaults payment_status to 'unpaid' when none is set")
    void save_defaultsPaymentStatusToUnpaid() throws Exception {
        Invoice invoice = sampleInvoice();
        assertNull(invoice.getPaymentStatus());

        dao.save(invoice);

        // Unlike LabOrderDAOImpl.save(), InvoiceDAOImpl does NOT write the DB-applied
        // default back onto the passed-in object either — same behavior, different table.
        assertNull(invoice.getPaymentStatus());
    }

    @Test
    @DisplayName("save rejects a negative total_amount — CHECK (total_amount >= 0)")
    void save_rejectsNegativeTotalAmount() {
        Invoice invoice = sampleInvoice();
        invoice.setTotalAmount(new BigDecimal("-1.00"));

        assertThrows(DatabaseException.class, () -> dao.save(invoice));
    }

    @Test
    @DisplayName("save rejects a payment_status outside the CHECK constraint's allowed values")
    void save_rejectsInvalidPaymentStatus() {
        Invoice invoice = sampleInvoice();
        invoice.setPaymentStatus("refunded");

        assertThrows(DatabaseException.class, () -> dao.save(invoice));
    }

    /**
     * invoices.appointment_id has NO UNIQUE constraint in hospital_schema.sql, even though
     * InvoiceServiceImpl.generate() assumes at-most-one-invoice-per-appointment via a
     * check-then-insert (findByAppointmentId then save) — the same class of gap already
     * documented for patients.email in PatientDAOImplTest. This test documents the DAO's
     * actual (permissive) behavior; a race between two concurrent generate() calls for the
     * same appointment could both pass the check and both insert.
     */
    @Test
    @DisplayName("save does not itself reject a second invoice for the same appointment — no UNIQUE(appointment_id) at the DB level")
    void save_allowsDuplicateAppointmentId_atDaoLevel() throws Exception {
        dao.save(sampleInvoice());

        assertDoesNotThrow(() -> dao.save(sampleInvoice()));
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns the saved invoice with every field intact")
    void findById_returnsSavedInvoice() throws Exception {
        Invoice saved = dao.save(sampleInvoice());

        Optional<Invoice> found = dao.findById(saved.getInvoiceId());

        assertTrue(found.isPresent());
        assertEquals(patientId, found.get().getPatientId());
        assertEquals(0, new BigDecimal("150.00").compareTo(found.get().getTotalAmount()));
        assertEquals("unpaid", found.get().getPaymentStatus());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted invoice")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        Invoice saved = dao.save(sampleInvoice());
        dao.softDelete(saved.getInvoiceId());

        assertTrue(dao.findById(saved.getInvoiceId()).isEmpty());
    }

    // ── findByAppointmentId ───────────────────────────────────────────────

    @Test
    @DisplayName("findByAppointmentId finds the invoice for that appointment")
    void findByAppointmentId_findsMatch() throws Exception {
        dao.save(sampleInvoice());

        Optional<Invoice> found = dao.findByAppointmentId(appointmentId);

        assertTrue(found.isPresent());
        assertEquals(appointmentId, found.get().getAppointmentId());
    }

    @Test
    @DisplayName("findByAppointmentId returns empty when no invoice exists for that appointment")
    void findByAppointmentId_returnsEmpty_whenNone() throws Exception {
        assertTrue(dao.findByAppointmentId(appointmentId).isEmpty());
    }

    // ── findByPatientId ───────────────────────────────────────────────────

    @Test
    @DisplayName("findByPatientId returns every non-deleted invoice for that patient, newest first")
    void findByPatientId_returnsInvoices() throws Exception {
        dao.save(sampleInvoice());

        List<Invoice> invoices = dao.findByPatientId(patientId);

        assertEquals(1, invoices.size());
        assertEquals(patientId, invoices.get(0).getPatientId());
    }

    @Test
    @DisplayName("findByPatientId excludes soft-deleted invoices")
    void findByPatientId_excludesSoftDeleted() throws Exception {
        Invoice saved = dao.save(sampleInvoice());
        dao.softDelete(saved.getInvoiceId());

        assertTrue(dao.findByPatientId(patientId).isEmpty());
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll returns every non-deleted invoice")
    void findAll_returnsNonDeletedInvoices() throws Exception {
        dao.save(sampleInvoice());
        Invoice toDelete = dao.save(sampleInvoice());
        dao.softDelete(toDelete.getInvoiceId());

        PageResult<Invoice> page = dao.findAll(CursorPagination.firstPage());

        assertEquals(1, page.getCount());
    }

    // ── updatePaymentStatus ───────────────────────────────────────────────

    @Test
    @DisplayName("updatePaymentStatus persists the new status and refreshes updated_at via the DB trigger")
    void updatePaymentStatus_persistsNewStatus() throws Exception {
        Invoice saved = dao.save(sampleInvoice());

        Invoice updated = dao.updatePaymentStatus(saved.getInvoiceId(), "paid");

        assertEquals("paid", updated.getPaymentStatus());
        assertEquals("paid", dao.findById(saved.getInvoiceId()).get().getPaymentStatus());
    }

    @Test
    @DisplayName("updatePaymentStatus throws ResourceNotFoundException for an id that doesn't exist")
    void updatePaymentStatus_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class,
                () -> dao.updatePaymentStatus(UUID.randomUUID().toString(), "paid"));
    }

    @Test
    @DisplayName("updatePaymentStatus wraps a CHECK-constraint violation as DatabaseException")
    void updatePaymentStatus_wrapsCheckViolation_asDatabaseException() throws Exception {
        Invoice saved = dao.save(sampleInvoice());

        assertThrows(DatabaseException.class, () -> dao.updatePaymentStatus(saved.getInvoiceId(), "refunded"));
    }

    // ── softDelete ────────────────────────────────────────────────────────

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        Invoice saved = dao.save(sampleInvoice());

        dao.softDelete(saved.getInvoiceId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getInvoiceId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }

    @Test
    @DisplayName("softDelete throws ResourceNotFoundException for an id that doesn't exist")
    void softDelete_throwsResourceNotFoundException_whenMissing() {
        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(UUID.randomUUID().toString()));
    }
}
