package hospital.management.backend.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceSummaryDTO {

    private String        invoiceId;
    private String        patientName;
    private BigDecimal    totalAmount;
    private String        paymentStatus;
    private LocalDateTime issuedAt;

    public InvoiceSummaryDTO() {}

    public InvoiceSummaryDTO(String invoiceId, String patientName, BigDecimal totalAmount,
                             String paymentStatus, LocalDateTime issuedAt) {
        this.invoiceId     = invoiceId;
        this.patientName   = patientName;
        this.totalAmount   = totalAmount;
        this.paymentStatus = paymentStatus;
        this.issuedAt      = issuedAt;
    }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    @Override
    public String toString() {
        return "InvoiceSummaryDTO{invoiceId='" + invoiceId + "', paymentStatus='" + paymentStatus + "'}";
    }
}