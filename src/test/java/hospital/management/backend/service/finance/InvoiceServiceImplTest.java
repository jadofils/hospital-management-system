package hospital.management.backend.service.finance;

import hospital.management.backend.dao.finance.interfaces.InvoiceDAO;
import hospital.management.backend.dao.patient.interfaces.PatientDAO;
import hospital.management.backend.dto.finance.CreateInvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceSummaryDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Every id used here is a fresh random UUID rather than a fixed literal — findById() reads
 * through a real, JVM-wide, static L1 in-process cache with no reset hook exposed to tests,
 * so a fixed id would risk one test's cached DTO leaking into another test's assertions
 * (see PatientServiceImplTest for the same rationale).
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock private InvoiceDAO invoiceDAO;
    @Mock private PatientDAO patientDAO;

    private InvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InvoiceServiceImpl(invoiceDAO, patientDAO);
    }

    private Invoice sampleInvoice(String id, String appointmentId, String patientId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setAppointmentId(appointmentId);
        invoice.setPatientId(patientId);
        invoice.setTotalAmount(new BigDecimal("150.00"));
        invoice.setPaymentStatus("unpaid");
        invoice.setIssuedAt(LocalDateTime.now());
        return invoice;
    }

    // ── generate ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("generate throws IllegalArgumentException when appointmentId is blank")
    void generate_throwsIllegalArgumentException_whenAppointmentIdBlank() {
        CreateInvoiceDTO dto = new CreateInvoiceDTO(" ", UUID.randomUUID().toString(), new BigDecimal("100"));

        assertThrows(IllegalArgumentException.class, () -> service.generate(dto));
        verifyNoInteractions(invoiceDAO);
    }

    @Test
    @DisplayName("generate throws IllegalArgumentException when appointmentId is not a valid UUID")
    void generate_throwsIllegalArgumentException_whenAppointmentIdNotUuid() {
        CreateInvoiceDTO dto = new CreateInvoiceDTO("not-a-uuid", UUID.randomUUID().toString(), new BigDecimal("100"));

        assertThrows(IllegalArgumentException.class, () -> service.generate(dto));
        verifyNoInteractions(invoiceDAO);
    }

    @Test
    @DisplayName("generate throws IllegalArgumentException when patientId is not a valid UUID")
    void generate_throwsIllegalArgumentException_whenPatientIdNotUuid() {
        CreateInvoiceDTO dto = new CreateInvoiceDTO(UUID.randomUUID().toString(), "not-a-uuid", new BigDecimal("100"));

        assertThrows(IllegalArgumentException.class, () -> service.generate(dto));
        verifyNoInteractions(invoiceDAO);
    }

    @Test
    @DisplayName("generate throws ValidationException when totalAmount is null")
    void generate_throwsValidationException_whenTotalAmountNull() {
        CreateInvoiceDTO dto = new CreateInvoiceDTO(UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);

        assertThrows(ValidationException.class, () -> service.generate(dto));
        verifyNoInteractions(invoiceDAO);
    }

    @Test
    @DisplayName("generate throws ValidationException when totalAmount is negative")
    void generate_throwsValidationException_whenTotalAmountNegative() {
        CreateInvoiceDTO dto = new CreateInvoiceDTO(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), new BigDecimal("-1.00"));

        assertThrows(ValidationException.class, () -> service.generate(dto));
        verifyNoInteractions(invoiceDAO);
    }

    @Test
    @DisplayName("generate throws ValidationException when an invoice already exists for the appointment")
    void generate_throwsValidationException_whenInvoiceAlreadyExists() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        String patientId = UUID.randomUUID().toString();
        when(invoiceDAO.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(sampleInvoice(UUID.randomUUID().toString(), appointmentId, patientId)));
        CreateInvoiceDTO dto = new CreateInvoiceDTO(appointmentId, patientId, new BigDecimal("100"));

        assertThrows(ValidationException.class, () -> service.generate(dto));
        verify(invoiceDAO, never()).save(any());
    }

    @Test
    @DisplayName("generate saves a new invoice defaulting its payment status to 'unpaid' when everything is valid")
    void generate_savesInvoice_whenValid() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        String patientId = UUID.randomUUID().toString();
        when(invoiceDAO.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(invoiceDAO.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setInvoiceId(UUID.randomUUID().toString());
            i.setIssuedAt(LocalDateTime.now());
            return i;
        });
        CreateInvoiceDTO dto = new CreateInvoiceDTO(appointmentId, patientId, new BigDecimal("150.00"));

        InvoiceDTO result = service.generate(dto);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceDAO).save(captor.capture());
        assertEquals("unpaid", captor.getValue().getPaymentStatus());
        assertEquals(0, new BigDecimal("150.00").compareTo(result.getTotalAmount()));
        assertEquals(patientId, result.getPatientId());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns a mapped DTO when the DAO finds a matching invoice")
    void findById_returnsMappedDto_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(invoiceDAO.findById(id))
                .thenReturn(Optional.of(sampleInvoice(id, UUID.randomUUID().toString(), UUID.randomUUID().toString())));

        InvoiceDTO dto = service.findById(id);

        assertEquals(id, dto.getInvoiceId());
        assertEquals("unpaid", dto.getPaymentStatus());
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the DAO finds nothing")
    void findById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(invoiceDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll maps each invoice to a summary DTO enriched with the patient's name")
    void findAll_mapsToSummaryDto_withPatientName() throws Exception {
        String patientId = UUID.randomUUID().toString();
        Invoice invoice = sampleInvoice(UUID.randomUUID().toString(), UUID.randomUUID().toString(), patientId);
        PageResult<Invoice> page = CursorPagination.toResult(
                List.of(invoice), CursorPagination.firstPage(), Invoice::getIssuedAt);
        when(invoiceDAO.findAll(any())).thenReturn(page);
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setFirstName("Jane");
        patient.setLastName("Doe");
        when(patientDAO.findById(patientId)).thenReturn(Optional.of(patient));

        PageResult<InvoiceSummaryDTO> result = service.findAll(CursorPagination.firstPage());

        assertEquals(1, result.getCount());
        assertEquals("Jane Doe", result.getItems().get(0).getPatientName());
    }

    @Test
    @DisplayName("findAll tolerates a patient lookup failure by leaving patientName null instead of throwing")
    void findAll_toleratesPatientLookupFailure() throws Exception {
        String patientId = UUID.randomUUID().toString();
        Invoice invoice = sampleInvoice(UUID.randomUUID().toString(), UUID.randomUUID().toString(), patientId);
        PageResult<Invoice> page = CursorPagination.toResult(
                List.of(invoice), CursorPagination.firstPage(), Invoice::getIssuedAt);
        when(invoiceDAO.findAll(any())).thenReturn(page);
        when(patientDAO.findById(patientId)).thenThrow(new RuntimeException("boom"));

        PageResult<InvoiceSummaryDTO> result = service.findAll(CursorPagination.firstPage());

        assertEquals(1, result.getCount());
        assertNull(result.getItems().get(0).getPatientName());
    }

    // ── findByPatient ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findByPatient maps every DAO invoice to a DTO")
    void findByPatient_mapsEveryInvoice() throws Exception {
        String patientId = UUID.randomUUID().toString();
        Invoice invoice1 = sampleInvoice(UUID.randomUUID().toString(), UUID.randomUUID().toString(), patientId);
        Invoice invoice2 = sampleInvoice(UUID.randomUUID().toString(), UUID.randomUUID().toString(), patientId);
        when(invoiceDAO.findByPatientId(patientId)).thenReturn(List.of(invoice1, invoice2));

        List<InvoiceDTO> result = service.findByPatient(patientId);

        assertEquals(2, result.size());
    }

    // ── markPaid ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("markPaid throws ResourceNotFoundException when the invoice doesn't exist")
    void markPaid_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(invoiceDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.markPaid(id));
        verify(invoiceDAO, never()).updatePaymentStatus(anyString(), anyString());
    }

    @Test
    @DisplayName("markPaid updates the payment status to 'paid' when the invoice exists")
    void markPaid_updatesStatusToPaid_whenInvoiceExists() throws Exception {
        String id = UUID.randomUUID().toString();
        String patientId = UUID.randomUUID().toString();
        Invoice existing = sampleInvoice(id, UUID.randomUUID().toString(), patientId);
        when(invoiceDAO.findById(id)).thenReturn(Optional.of(existing));
        Invoice paid = sampleInvoice(id, existing.getAppointmentId(), patientId);
        paid.setPaymentStatus("paid");
        when(invoiceDAO.updatePaymentStatus(id, "paid")).thenReturn(paid);

        InvoiceDTO result = service.markPaid(id);

        assertEquals("paid", result.getPaymentStatus());
        verify(invoiceDAO).updatePaymentStatus(id, "paid");
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete throws ResourceNotFoundException when the invoice doesn't exist")
    void delete_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(invoiceDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
        verify(invoiceDAO, never()).softDelete(anyString());
    }

    @Test
    @DisplayName("delete delegates to the DAO's soft-delete when the invoice exists")
    void delete_softDeletes_whenInvoiceExists() throws Exception {
        String id = UUID.randomUUID().toString();
        Invoice existing = sampleInvoice(id, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        when(invoiceDAO.findById(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        verify(invoiceDAO).softDelete(id);
    }
}
