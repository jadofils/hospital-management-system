package hospital.management.backend.service.finance;

import com.fasterxml.jackson.core.type.TypeReference;
import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.clinical.interfaces.AppointmentDAO;
import hospital.management.backend.dao.finance.interfaces.InvoiceDAO;
import hospital.management.backend.dao.patient.interfaces.PatientDAO;
import hospital.management.backend.dto.finance.CreateInvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceSummaryDTO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.finance.InvoiceMapper;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.service.finance.interfaces.InvoiceService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InvoiceServiceImpl implements InvoiceService {

    private static final String STATUS_PAID = "paid";

    private final InvoiceDAO      invoiceDAO;
    private final PatientDAO      patientDAO;
    private final AppointmentDAO  appointmentDAO;

    public InvoiceServiceImpl(InvoiceDAO invoiceDAO, PatientDAO patientDAO, AppointmentDAO appointmentDAO) {
        this.invoiceDAO     = invoiceDAO;
        this.patientDAO     = patientDAO;
        this.appointmentDAO = appointmentDAO;
    }

    @Override
    public InvoiceDTO generate(CreateInvoiceDTO dto) throws Exception {
        String appointmentId = ValidatorUtils.requireNonBlank(dto.getAppointmentId(), "appointmentId");
        ValidatorUtils.requireValidUuid(appointmentId, "appointmentId");
        String patientId = ValidatorUtils.requireNonBlank(dto.getPatientId(), "patientId");
        ValidatorUtils.requireValidUuid(patientId, "patientId");
        if (dto.getTotalAmount() == null || dto.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("totalAmount", "Total amount must be zero or greater.");
        }

        // An invoice can only be issued for an appointment that belongs to this patient.
        Appointment appointment = appointmentDAO.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        if (!patientId.equals(appointment.getPatientId())) {
            throw new ValidationException("appointmentId",
                    "The selected appointment does not belong to the selected patient.");
        }

        if (invoiceDAO.findByAppointmentId(appointmentId).isPresent()) {
            throw new ValidationException("appointmentId", "An invoice already exists for this appointment.");
        }

        CacheService.evict(CacheKey.invoicesByPatient(patientId));
        CacheService.evictByPattern(CacheKey.ALL_INVOICES);

        // The check above is a friendly pre-check, not the guarantee — two concurrent
        // generate() calls for the same appointment can both pass it. The DB-level unique
        // index (uq_invoices_appointment_active) is the authoritative guard; a race that
        // slips past the pre-check surfaces here as a constraint violation instead of a
        // silent duplicate invoice.
        Invoice saved;
        try {
            saved = invoiceDAO.save(InvoiceMapper.toEntity(dto));
        } catch (DatabaseException e) {
            if (isUniqueViolation(e)) {
                throw new ValidationException("appointmentId", "An invoice already exists for this appointment.");
            }
            throw e;
        }
        InvoiceDTO result = InvoiceMapper.toDTO(saved);
        CacheService.set(CacheKey.invoice(saved.getInvoiceId()), result, CacheDomain.INVOICE);
        EventBus.publish(AppEventType.INVOICE_CREATED, saved.getInvoiceId());
        return result;
    }

    /** True if a DatabaseException was caused by a Postgres unique_violation (SQLSTATE 23505). */
    private static boolean isUniqueViolation(DatabaseException e) {
        Throwable cause = e.getCause();
        return cause instanceof java.sql.SQLException sqlEx && "23505".equals(sqlEx.getSQLState());
    }

    @Override
    public InvoiceDTO findById(String invoiceId) throws Exception {
        Optional<InvoiceDTO> cached = CacheService.get(CacheKey.invoice(invoiceId), InvoiceDTO.class);
        if (cached.isPresent()) return cached.get();

        Invoice invoice = invoiceDAO.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        InvoiceDTO dto = InvoiceMapper.toDTO(invoice);
        CacheService.set(CacheKey.invoice(invoiceId), dto, CacheDomain.INVOICE);
        return dto;
    }

    @Override
    public Optional<InvoiceDTO> findByAppointment(String appointmentId) throws Exception {
        return invoiceDAO.findByAppointmentId(appointmentId).map(InvoiceMapper::toDTO);
    }

    @Override
    public PageResult<InvoiceSummaryDTO> findAll(PageRequest request) throws Exception {
        // Cursor-based pagination has no stable page number to key a cache entry on
        // (CacheKey.invoiceList(page, size) assumes classic offset paging), so this
        // follows the same uncached pass-through as DoctorServiceImpl.findAll.
        PageResult<Invoice> page = invoiceDAO.findAll(request);

        Map<String, String> patientNames = new HashMap<>();
        for (Invoice invoice : page.getItems()) {
            patientNames.computeIfAbsent(invoice.getPatientId(), pid -> {
                try {
                    return patientDAO.findById(pid).map(Patient::getFullName).orElse(null);
                } catch (Exception e) {
                    return null;
                }
            });
        }
        return page.map(invoice -> InvoiceMapper.toSummaryDTO(invoice, patientNames.get(invoice.getPatientId())));
    }

    @Override
    public List<InvoiceDTO> findByPatient(String patientId) throws Exception {
        Optional<List<InvoiceDTO>> cached = CacheService.get(
                CacheKey.invoicesByPatient(patientId),
                new TypeReference<List<InvoiceDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<InvoiceDTO> dtos = new ArrayList<>();
        for (Invoice invoice : invoiceDAO.findByPatientId(patientId)) dtos.add(InvoiceMapper.toDTO(invoice));
        CacheService.set(CacheKey.invoicesByPatient(patientId), dtos, CacheDomain.INVOICE);
        return dtos;
    }

    @Override
    public InvoiceDTO markPaid(String invoiceId) throws Exception {
        Invoice invoice = invoiceDAO.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        CacheService.evict(CacheKey.invoice(invoiceId));
        CacheService.evict(CacheKey.invoicesByPatient(invoice.getPatientId()));
        CacheService.evictByPattern(CacheKey.ALL_INVOICES);

        Invoice updated = invoiceDAO.updatePaymentStatus(invoiceId, STATUS_PAID);
        InvoiceDTO dto = InvoiceMapper.toDTO(updated);
        CacheService.set(CacheKey.invoice(invoiceId), dto, CacheDomain.INVOICE);
        EventBus.publish(AppEventType.INVOICE_PAID, invoiceId);
        return dto;
    }

    @Override
    public void delete(String invoiceId) throws Exception {
        Invoice invoice = invoiceDAO.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        CacheService.evict(CacheKey.invoice(invoiceId));
        CacheService.evict(CacheKey.invoicesByPatient(invoice.getPatientId()));
        CacheService.evictByPattern(CacheKey.ALL_INVOICES);

        invoiceDAO.softDelete(invoiceId);
        EventBus.publish(AppEventType.INVOICE_UPDATED, invoiceId);
    }
}
