package hospital.management.backend.model.finance;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity model for the `invoices` table.
 * Uses JavaFX properties so TableView / PropertyValueFactory work without extra wiring.
 *
 * Receipt design note: a receipt is NOT a separate entity.
 * An invoice whose payment_status = 'paid' IS the receipt — print/export it as-is.
 */
public class Invoice {

    private final StringProperty invoiceId     = new SimpleStringProperty();
    private final StringProperty appointmentId = new SimpleStringProperty();
    private final StringProperty patientId     = new SimpleStringProperty();
    private final ObjectProperty<BigDecimal>    totalAmount   = new SimpleObjectProperty<>();
    private final StringProperty paymentStatus = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> issuedAt      = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updatedAt     = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> deletedAt     = new SimpleObjectProperty<>();

    public Invoice() {}

    public Invoice(String invoiceId, String appointmentId, String patientId,
                   BigDecimal totalAmount, String paymentStatus,
                   LocalDateTime issuedAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.invoiceId.set(invoiceId);
        this.appointmentId.set(appointmentId);
        this.patientId.set(patientId);
        this.totalAmount.set(totalAmount);
        this.paymentStatus.set(paymentStatus);
        this.issuedAt.set(issuedAt);
        this.updatedAt.set(updatedAt);
        this.deletedAt.set(deletedAt);
    }

    public String getInvoiceId()              { return invoiceId.get(); }
    public StringProperty invoiceIdProperty() { return invoiceId; }
    public void setInvoiceId(String v)        { invoiceId.set(v); }

    public String getAppointmentId()              { return appointmentId.get(); }
    public StringProperty appointmentIdProperty() { return appointmentId; }
    public void setAppointmentId(String v)        { appointmentId.set(v); }

    public String getPatientId()              { return patientId.get(); }
    public StringProperty patientIdProperty() { return patientId; }
    public void setPatientId(String v)        { patientId.set(v); }

    public BigDecimal getTotalAmount()                       { return totalAmount.get(); }
    public ObjectProperty<BigDecimal> totalAmountProperty() { return totalAmount; }
    public void setTotalAmount(BigDecimal v)                { totalAmount.set(v); }

    public String getPaymentStatus()              { return paymentStatus.get(); }
    public StringProperty paymentStatusProperty() { return paymentStatus; }
    public void setPaymentStatus(String v)        { paymentStatus.set(v); }

    public LocalDateTime getIssuedAt()                       { return issuedAt.get(); }
    public ObjectProperty<LocalDateTime> issuedAtProperty() { return issuedAt; }
    public void setIssuedAt(LocalDateTime v)                { issuedAt.set(v); }

    public LocalDateTime getUpdatedAt()                       { return updatedAt.get(); }
    public ObjectProperty<LocalDateTime> updatedAtProperty() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)                { updatedAt.set(v); }

    public LocalDateTime getDeletedAt()                       { return deletedAt.get(); }
    public ObjectProperty<LocalDateTime> deletedAtProperty() { return deletedAt; }
    public void setDeletedAt(LocalDateTime v)                { deletedAt.set(v); }

    @Override
    public String toString() {
        return "Invoice{invoiceId=" + getInvoiceId() +
               ", patientId=" + getPatientId() +
               ", totalAmount=" + getTotalAmount() +
               ", paymentStatus=" + getPaymentStatus() +
               ", issuedAt=" + getIssuedAt() + "}";
    }
}