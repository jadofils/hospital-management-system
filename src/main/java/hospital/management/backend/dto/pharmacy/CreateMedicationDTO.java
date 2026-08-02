package hospital.management.backend.dto.pharmacy;

import java.math.BigDecimal;

public class CreateMedicationDTO {

    private String     name;
    private String     genericName;
    private String     form;
    private BigDecimal unitPrice;

    public CreateMedicationDTO() {}

    public CreateMedicationDTO(String name, String genericName, String form, BigDecimal unitPrice) {
        this.name        = name;
        this.genericName = genericName;
        this.form        = form;
        this.unitPrice   = unitPrice;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    @Override
    public String toString() {
        return "CreateMedicationDTO{name='" + name + "'}";
    }
}