package hospital.management.backend.dao.finance.interfaces;

import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;
import java.util.Optional;

public interface InvoiceDAO {
    Invoice save(Invoice invoice) throws Exception;
    Optional<Invoice> findById(String invoiceId) throws Exception;
    Optional<Invoice> findByAppointmentId(String appointmentId) throws Exception;
    List<Invoice> findByPatientId(String patientId) throws Exception;
    PageResult<Invoice> findAll(PageRequest request) throws Exception;
    Invoice updatePaymentStatus(String invoiceId, String status) throws Exception;
    void softDelete(String invoiceId) throws Exception;
}