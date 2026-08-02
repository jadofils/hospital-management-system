package hospital.management.backend.dto.pharmacy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MedicationDTO {

    private String        medicationId;
    private String        name;
    private String        genericName;
    private String        form;
    private BigDecimal    unitPrice;
    private LocalDateTime createdAt;

    public MedicationDTO() {}

    public MedicationDTO(String medicationId, String name, String genericName,
                         String form, BigDecimal unitPrice, LocalDateTime createdAt) {
        this.medicationId = medicationId;
        this.name         = name;
        this.genericName  = genericName;
        this.form         = form;
        this.unitPrice    = unitPrice;
        this.createdAt    = createdAt;
    }

    public String getMedicationId() { return medicationId; }
    public void setMedicationId(String medicationId) { this.medicationId = medicationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "MedicationDTO{medicationId='" + medicationId + "', name='" + name + "'}";
    }
}