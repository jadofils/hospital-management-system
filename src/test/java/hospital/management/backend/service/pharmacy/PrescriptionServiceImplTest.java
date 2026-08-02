package hospital.management.backend.service.pharmacy;

import hospital.management.backend.config.db.TransactionManager;
import hospital.management.backend.dao.pharmacy.interfaces.PrescriptionDAO;
import hospital.management.backend.dao.pharmacy.interfaces.PrescriptionItemDAO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionDTO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionItemDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.pharmacy.Prescription;
import hospital.management.backend.model.pharmacy.PrescriptionItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * issue() wraps the prescription header write and every line-item write in
 * TransactionManager.executeInTransaction(...), a static method that normally opens a
 * real JDBC Connection — mocked here (via mockStatic) to just invoke the work lambda
 * directly with a stub Connection, so these tests never touch a real database (same
 * approach as AuthServiceImplTest). EventBus.publish(...) is left un-mocked: with no
 * listeners registered in this test process it's a guaranteed no-op.
 *
 * Ids are fresh random UUIDs to avoid the static L1 CacheService leaking cached DTOs
 * across tests (see PatientServiceImplTest for the same rationale).
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

    @Mock private PrescriptionDAO prescriptionDAO;
    @Mock private PrescriptionItemDAO itemDAO;

    private PrescriptionServiceImpl service;
    private MockedStatic<TransactionManager> transactionManagerMock;

    @BeforeEach
    void setUp() {
        service = new PrescriptionServiceImpl(prescriptionDAO, itemDAO);

        transactionManagerMock = mockStatic(TransactionManager.class);
        transactionManagerMock
                .when(() -> TransactionManager.executeInTransaction(any(TransactionManager.TransactionalWork.class)))
                .thenAnswer(invocation -> {
                    TransactionManager.TransactionalWork<?> work = invocation.getArgument(0);
                    return work.execute(mock(Connection.class));
                });
    }

    @AfterEach
    void tearDown() {
        transactionManagerMock.close();
    }

    private CreatePrescriptionItemDTO sampleItemDto(String medicationId) {
        return new CreatePrescriptionItemDTO(medicationId, "500mg twice daily", 20, "Take with food");
    }

    private Prescription samplePrescription(String id, String appointmentId) {
        Prescription p = new Prescription();
        p.setPrescriptionId(id);
        p.setAppointmentId(appointmentId);
        p.setDateIssued(LocalDate.now());
        return p;
    }

    private PrescriptionItem sampleItemEntity(String id, String prescriptionId, String medicationId) {
        PrescriptionItem item = new PrescriptionItem();
        item.setItemId(id);
        item.setPrescriptionId(prescriptionId);
        item.setMedicationId(medicationId);
        item.setDosage("500mg twice daily");
        item.setQuantity(20);
        item.setInstructions("Take with food");
        return item;
    }

    // ── issue ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("issue throws IllegalArgumentException when appointmentId is blank")
    void issue_throwsIllegalArgumentException_whenAppointmentIdBlank() {
        CreatePrescriptionDTO dto = new CreatePrescriptionDTO("  ", null,
                List.of(sampleItemDto(UUID.randomUUID().toString())));

        assertThrows(IllegalArgumentException.class, () -> service.issue(dto));
        verifyNoInteractions(prescriptionDAO, itemDAO);
    }

    @Test
    @DisplayName("issue throws IllegalArgumentException when appointmentId is not a valid UUID")
    void issue_throwsIllegalArgumentException_whenAppointmentIdNotUuid() {
        CreatePrescriptionDTO dto = new CreatePrescriptionDTO("not-a-uuid", null,
                List.of(sampleItemDto(UUID.randomUUID().toString())));

        assertThrows(IllegalArgumentException.class, () -> service.issue(dto));
        verifyNoInteractions(prescriptionDAO, itemDAO);
    }

    @Test
    @DisplayName("issue throws ValidationException when items is null")
    void issue_throwsValidationException_whenItemsNull() {
        CreatePrescriptionDTO dto = new CreatePrescriptionDTO(UUID.randomUUID().toString(), null, null);

        assertThrows(ValidationException.class, () -> service.issue(dto));
        verifyNoInteractions(prescriptionDAO, itemDAO);
    }

    @Test
    @DisplayName("issue throws ValidationException when items is empty")
    void issue_throwsValidationException_whenItemsEmpty() {
        CreatePrescriptionDTO dto = new CreatePrescriptionDTO(UUID.randomUUID().toString(), null, List.of());

        assertThrows(ValidationException.class, () -> service.issue(dto));
        verifyNoInteractions(prescriptionDAO, itemDAO);
    }

    @Test
    @DisplayName("issue throws IllegalArgumentException when an item's medicationId is blank")
    void issue_throwsIllegalArgumentException_whenItemMedicationIdBlank() {
        CreatePrescriptionDTO dto = new CreatePrescriptionDTO(UUID.randomUUID().toString(), null,
                List.of(sampleItemDto("  ")));

        assertThrows(IllegalArgumentException.class, () -> service.issue(dto));
        verifyNoInteractions(prescriptionDAO, itemDAO);
    }

    @Test
    @DisplayName("issue throws ValidationException when an item's quantity is zero")
    void issue_throwsValidationException_whenItemQuantityZero() {
        CreatePrescriptionItemDTO zeroQty = new CreatePrescriptionItemDTO(
                UUID.randomUUID().toString(), "dosage", 0, "instructions");
        CreatePrescriptionDTO dto = new CreatePrescriptionDTO(UUID.randomUUID().toString(), null, List.of(zeroQty));

        assertThrows(ValidationException.class, () -> service.issue(dto));
        verifyNoInteractions(prescriptionDAO, itemDAO);
    }

    @Test
    @DisplayName("issue throws ValidationException when an item's quantity is negative")
    void issue_throwsValidationException_whenItemQuantityNegative() {
        CreatePrescriptionItemDTO negativeQty = new CreatePrescriptionItemDTO(
                UUID.randomUUID().toString(), "dosage", -1, "instructions");
        CreatePrescriptionDTO dto = new CreatePrescriptionDTO(UUID.randomUUID().toString(), null, List.of(negativeQty));

        assertThrows(ValidationException.class, () -> service.issue(dto));
        verifyNoInteractions(prescriptionDAO, itemDAO);
    }

    @Test
    @DisplayName("issue throws ValidationException when an item's quantity is missing")
    void issue_throwsValidationException_whenItemQuantityMissing() {
        CreatePrescriptionItemDTO noQty = new CreatePrescriptionItemDTO(
                UUID.randomUUID().toString(), "dosage", null, "instructions");
        CreatePrescriptionDTO dto = new CreatePrescriptionDTO(UUID.randomUUID().toString(), null, List.of(noQty));

        assertThrows(ValidationException.class, () -> service.issue(dto));
        verifyNoInteractions(prescriptionDAO, itemDAO);
    }

    @Test
    @DisplayName("issue defaults dateIssued to today when not supplied, then saves the header and every item "
            + "inside a single transaction")
    void issue_savesHeaderAndItems_whenValid() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        String medicationId = UUID.randomUUID().toString();
        CreatePrescriptionDTO dto = new CreatePrescriptionDTO(appointmentId, null, List.of(sampleItemDto(medicationId)));

        when(prescriptionDAO.save(any(Prescription.class), any(Connection.class))).thenAnswer(inv -> {
            Prescription p = inv.getArgument(0);
            p.setPrescriptionId(UUID.randomUUID().toString());
            return p;
        });
        when(itemDAO.save(any(PrescriptionItem.class), any(Connection.class))).thenAnswer(inv -> {
            PrescriptionItem item = inv.getArgument(0);
            item.setItemId(UUID.randomUUID().toString());
            return item;
        });

        PrescriptionDTO result = service.issue(dto);

        assertEquals(appointmentId, result.getAppointmentId());
        assertEquals(LocalDate.now(), result.getDateIssued());
        assertNotNull(result.getPrescriptionId());
        assertEquals(1, result.getItems().size());
        assertEquals(medicationId, result.getItems().get(0).getMedicationId());
        verify(prescriptionDAO).save(any(Prescription.class), any(Connection.class));
        verify(itemDAO).save(any(PrescriptionItem.class), any(Connection.class));
    }

    @Test
    @DisplayName("issue saves multiple items under the same prescription header")
    void issue_savesMultipleItems() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        CreatePrescriptionDTO dto = new CreatePrescriptionDTO(appointmentId, LocalDate.now(), List.of(
                sampleItemDto(UUID.randomUUID().toString()),
                sampleItemDto(UUID.randomUUID().toString())));

        when(prescriptionDAO.save(any(Prescription.class), any(Connection.class))).thenAnswer(inv -> {
            Prescription p = inv.getArgument(0);
            p.setPrescriptionId(UUID.randomUUID().toString());
            return p;
        });
        when(itemDAO.save(any(PrescriptionItem.class), any(Connection.class))).thenAnswer(inv -> {
            PrescriptionItem item = inv.getArgument(0);
            item.setItemId(UUID.randomUUID().toString());
            return item;
        });

        PrescriptionDTO result = service.issue(dto);

        assertEquals(2, result.getItems().size());
        verify(itemDAO, times(2)).save(any(PrescriptionItem.class), any(Connection.class));
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns a mapped DTO with its items when the DAO finds a matching prescription")
    void findById_returnsMappedDtoWithItems_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        String appointmentId = UUID.randomUUID().toString();
        String medicationId = UUID.randomUUID().toString();
        when(prescriptionDAO.findById(id)).thenReturn(Optional.of(samplePrescription(id, appointmentId)));
        when(itemDAO.findByPrescriptionId(id)).thenReturn(List.of(
                sampleItemEntity(UUID.randomUUID().toString(), id, medicationId)));

        PrescriptionDTO dto = service.findById(id);

        assertEquals(id, dto.getPrescriptionId());
        assertEquals(1, dto.getItems().size());
        assertEquals(medicationId, dto.getItems().get(0).getMedicationId());
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the DAO finds nothing")
    void findById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(prescriptionDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    // ── findByAppointment ─────────────────────────────────────────────────

    @Test
    @DisplayName("findByAppointment returns a mapped DTO with its items when the DAO finds a match")
    void findByAppointment_returnsMappedDto_whenFound() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        String prescriptionId = UUID.randomUUID().toString();
        when(prescriptionDAO.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(samplePrescription(prescriptionId, appointmentId)));
        when(itemDAO.findByPrescriptionId(prescriptionId)).thenReturn(List.of());

        PrescriptionDTO dto = service.findByAppointment(appointmentId);

        assertEquals(prescriptionId, dto.getPrescriptionId());
        assertTrue(dto.getItems().isEmpty());
    }

    @Test
    @DisplayName("findByAppointment throws ResourceNotFoundException when no prescription was issued")
    void findByAppointment_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        when(prescriptionDAO.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByAppointment(appointmentId));
    }

    // ── findByPatient ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findByPatient maps every DAO prescription (with its items) to a DTO")
    void findByPatient_mapsEveryPrescriptionWithItems() throws Exception {
        String patientId = UUID.randomUUID().toString();
        String prescriptionId1 = UUID.randomUUID().toString();
        String prescriptionId2 = UUID.randomUUID().toString();
        when(prescriptionDAO.findByPatientId(patientId)).thenReturn(List.of(
                samplePrescription(prescriptionId1, UUID.randomUUID().toString()),
                samplePrescription(prescriptionId2, UUID.randomUUID().toString())));
        when(itemDAO.findByPrescriptionId(prescriptionId1)).thenReturn(List.of());
        when(itemDAO.findByPrescriptionId(prescriptionId2)).thenReturn(List.of());

        List<PrescriptionDTO> result = service.findByPatient(patientId);

        assertEquals(2, result.size());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete throws ResourceNotFoundException when the prescription doesn't exist")
    void delete_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(prescriptionDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
        verify(prescriptionDAO, never()).softDelete(anyString());
    }

    @Test
    @DisplayName("delete soft-deletes the prescription once it's confirmed to exist")
    void delete_softDeletes_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(prescriptionDAO.findById(id)).thenReturn(
                Optional.of(samplePrescription(id, UUID.randomUUID().toString())));

        service.delete(id);

        verify(prescriptionDAO).softDelete(id);
    }
}
