package hospital.management.backend.mapper.finance;

import hospital.management.backend.dto.finance.CreateInvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceSummaryDTO;
import hospital.management.backend.model.finance.Invoice;

public class InvoiceMapper {

    public static InvoiceDTO toDTO(Invoice invoice) {
        if (invoice == null) return null;
        return new InvoiceDTO(
            invoice.getInvoiceId(),
            invoice.getAppointmentId(),
            invoice.getPatientId(),
            invoice.getTotalAmount(),
            invoice.getPaymentStatus(),
            invoice.getIssuedAt()
        );
    }

    public static InvoiceSummaryDTO toSummaryDTO(Invoice invoice, String patientName) {
        if (invoice == null) return null;
        return new InvoiceSummaryDTO(
            invoice.getInvoiceId(),
            patientName,
            invoice.getTotalAmount(),
            invoice.getPaymentStatus(),
            invoice.getIssuedAt()
        );
    }

    public static Invoice toEntity(CreateInvoiceDTO dto) {
        if (dto == null) return null;
        Invoice invoice = new Invoice();
        invoice.setAppointmentId(dto.getAppointmentId());
        invoice.setPatientId(dto.getPatientId());
        invoice.setTotalAmount(dto.getTotalAmount());
        invoice.setPaymentStatus("unpaid");
        return invoice;
    }
}