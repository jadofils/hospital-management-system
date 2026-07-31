package hospital.management.backend.dto.finance;

import java.math.BigDecimal;

public class CreateInvoiceDTO {

    private String     appointmentId;
    private String     patientId;
    private BigDecimal totalAmount;

    public CreateInvoiceDTO() {}

    public CreateInvoiceDTO(String appointmentId, String patientId, BigDecimal totalAmount) {
        this.appointmentId = appointmentId;
        this.patientId     = patientId;
        this.totalAmount   = totalAmount;
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    @Override
    public String toString() {
        return "CreateInvoiceDTO{appointmentId='" + appointmentId + "', totalAmount=" + totalAmount + "}";
    }
}