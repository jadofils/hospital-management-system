package hospital.management.backend.dao.pharmacy;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.pharmacy.Prescription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion here
 * runs PrescriptionDAOImpl's actual SQL against a real database, proving the RETURNING
 * clauses, the appointment_id FK (RESTRICT), and the connection-accepting overload used
 * by PrescriptionServiceImpl.issue() to compose a single transaction with prescription_items.
 *
 * A prescription always hangs off a real appointment (patient + doctor), so every test
 * seeds that FK chain directly via JDBC rather than depending on another domain's DAO.
 */
class PrescriptionDAOImplTest extends PostgresIntegrationTestBase {

    private final PrescriptionDAOImpl dao = new PrescriptionDAOImpl();

    /** Inserts a minimal patient row and returns its generated id. */
    private String insertPatient() throws Exception {
        String sql = "INSERT INTO patients (first_name, last_name, dob) VALUES (?, ?, ?) RETURNING patient_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "Jane");
            ps.setString(2, "Doe");
            ps.setObject(3, java.sql.Date.valueOf(LocalDate.of(1990, 5, 20)));
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject("patient_id", UUID.class).toString();
            }
        }
    }

    /** Inserts a minimal doctor row and returns its generated id. */
    private String insertDoctor() throws Exception {
        String sql = "INSERT INTO doctors (first_name, last_name) VALUES (?, ?) RETURNING doctor_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "Greg");
            ps.setString(2, "House");
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject("doctor_id", UUID.class).toString();
            }
        }
    }

    /** Inserts a minimal appointment row (with its own fresh patient+doctor) and returns its id. */
    private String insertAppointment() throws Exception {
        return insertAppointment(insertPatient(), insertDoctor());
    }

    private String insertAppointment(String patientId, String doctorId) throws Exception {
        String sql = "INSERT INTO appointments (patient_id, doctor_id, appointment_date) "
                   + "VALUES (?, ?, ?) RETURNING appointment_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(patientId));
            ps.setObject(2, UUID.fromString(doctorId));
            ps.setObject(3, java.sql.Timestamp.valueOf(LocalDate.now().atStartOfDay()));
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject("appointment_id", UUID.class).toString();
            }
        }
    }

    private Prescription samplePrescription(String appointmentId) {
        Prescription p = new Prescription();
        p.setAppointmentId(appointmentId);
        p.setDateIssued(LocalDate.now());
        return p;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        String appointmentId = insertAppointment();

        Prescription saved = dao.save(samplePrescription(appointmentId));

        assertNotNull(saved.getPrescriptionId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getPrescriptionId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save(prescription, conn) runs on the caller-supplied connection, without committing itself")
    void save_withConnection_usesCallerSuppliedConnection() throws Exception {
        String appointmentId = insertAppointment();

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            Prescription saved = dao.save(samplePrescription(appointmentId), conn);
            conn.commit();

            assertNotNull(saved.getPrescriptionId());
            assertTrue(dao.findById(saved.getPrescriptionId()).isPresent());
        }
    }

    @Test
    @DisplayName("save rejects an appointment_id that doesn't exist via the FK constraint")
    void save_rejectsUnknownAppointmentId() {
        Prescription orphan = samplePrescription(UUID.randomUUID().toString());

        assertThrows(DatabaseException.class, () -> dao.save(orphan));
    }

    @Test
    @DisplayName("findById returns the saved prescription with every field intact")
    void findById_returnsSavedPrescription() throws Exception {
        String appointmentId = insertAppointment();
        Prescription saved = dao.save(samplePrescription(appointmentId));

        Optional<Prescription> found = dao.findById(saved.getPrescriptionId());

        assertTrue(found.isPresent());
        assertEquals(appointmentId, found.get().getAppointmentId());
        assertEquals(LocalDate.now(), found.get().getDateIssued());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted prescription")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        String appointmentId = insertAppointment();
        Prescription saved = dao.save(samplePrescription(appointmentId));
        dao.softDelete(saved.getPrescriptionId());

        assertTrue(dao.findById(saved.getPrescriptionId()).isEmpty());
    }

    @Test
    @DisplayName("findByAppointmentId finds the prescription issued for that appointment")
    void findByAppointmentId_findsMatch() throws Exception {
        String appointmentId = insertAppointment();
        Prescription saved = dao.save(samplePrescription(appointmentId));

        Optional<Prescription> found = dao.findByAppointmentId(appointmentId);

        assertTrue(found.isPresent());
        assertEquals(saved.getPrescriptionId(), found.get().getPrescriptionId());
    }

    @Test
    @DisplayName("findByAppointmentId returns empty when no prescription was issued for that appointment")
    void findByAppointmentId_returnsEmpty_whenNoneIssued() throws Exception {
        String appointmentId = insertAppointment();

        assertTrue(dao.findByAppointmentId(appointmentId).isEmpty());
    }

    @Test
    @DisplayName("findByPatientId joins through appointments and returns every prescription for that patient, "
            + "most recently issued first")
    void findByPatientId_joinsThroughAppointments() throws Exception {
        String patientId = insertPatient();
        String doctorId = insertDoctor();
        String appointment1 = insertAppointment(patientId, doctorId);
        String appointment2 = insertAppointment(patientId, doctorId);
        String otherPatientAppointment = insertAppointment();

        Prescription older = samplePrescription(appointment1);
        older.setDateIssued(LocalDate.now().minusDays(5));
        dao.save(older);

        Prescription newer = samplePrescription(appointment2);
        newer.setDateIssued(LocalDate.now());
        dao.save(newer);

        dao.save(samplePrescription(otherPatientAppointment));

        List<Prescription> result = dao.findByPatientId(patientId);

        assertEquals(2, result.size());
        assertEquals(LocalDate.now(), result.get(0).getDateIssued());
        assertEquals(LocalDate.now().minusDays(5), result.get(1).getDateIssued());
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        String appointmentId = insertAppointment();
        Prescription saved = dao.save(samplePrescription(appointmentId));

        dao.softDelete(saved.getPrescriptionId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getPrescriptionId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }
}
