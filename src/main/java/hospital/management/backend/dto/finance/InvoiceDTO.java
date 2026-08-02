package hospital.management.backend.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceDTO {

    private String        invoiceId;
    private String        appointmentId;
    private String        patientId;
    private BigDecimal    totalAmount;
    private String        paymentStatus;
    private LocalDateTime issuedAt;

    public InvoiceDTO() {}

    public InvoiceDTO(String invoiceId, String appointmentId, String patientId,
                      BigDecimal totalAmount, String paymentStatus, LocalDateTime issuedAt) {
        this.invoiceId     = invoiceId;
        this.appointmentId = appointmentId;
        this.patientId     = patientId;
        this.totalAmount   = totalAmount;
        this.paymentStatus = paymentStatus;
        this.issuedAt      = issuedAt;
    }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    @Override
    public String toString() {
        return "InvoiceDTO{invoiceId='" + invoiceId + "', paymentStatus='" + paymentStatus + "'}";
    }
}