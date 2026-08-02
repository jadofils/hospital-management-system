package hospital.management.backend.dto.pharmacy;

public class PrescriptionItemDTO {

    private String  itemId;
    private String  prescriptionId;
    private String  medicationId;
    private String  dosage;
    private Integer quantity;
    private String  instructions;

    public PrescriptionItemDTO() {}

    public PrescriptionItemDTO(String itemId, String prescriptionId, String medicationId,
                               String dosage, Integer quantity, String instructions) {
        this.itemId         = itemId;
        this.prescriptionId = prescriptionId;
        this.medicationId   = medicationId;
        this.dosage         = dosage;
        this.quantity       = quantity;
        this.instructions   = instructions;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }

    public String getMedicationId() { return medicationId; }
    public void setMedicationId(String medicationId) { this.medicationId = medicationId; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    @Override
    public String toString() {
        return "PrescriptionItemDTO{itemId='" + itemId + "', medicationId='" + medicationId + "'}";
    }
}