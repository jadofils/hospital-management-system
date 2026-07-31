package hospital.management.backend.service.finance.interfaces;

import hospital.management.backend.dto.finance.CreateInvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceSummaryDTO;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;

public interface InvoiceService {
    InvoiceDTO generate(CreateInvoiceDTO dto) throws Exception;
    InvoiceDTO findById(String invoiceId) throws Exception;
    PageResult<InvoiceSummaryDTO> findAll(PageRequest request) throws Exception;
    List<InvoiceDTO> findByPatient(String patientId) throws Exception;
    InvoiceDTO markPaid(String invoiceId) throws Exception;
    void delete(String invoiceId) throws Exception;
}