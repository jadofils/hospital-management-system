package hospital.management.backend.dto.pharmacy;

public class CreatePrescriptionItemDTO {

    private String  medicationId;
    private String  dosage;
    private Integer quantity;
    private String  instructions;

    public CreatePrescriptionItemDTO() {}

    public CreatePrescriptionItemDTO(String medicationId, String dosage,
                                     Integer quantity, String instructions) {
        this.medicationId = medicationId;
        this.dosage       = dosage;
        this.quantity     = quantity;
        this.instructions = instructions;
    }

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
        return "CreatePrescriptionItemDTO{medicationId='" + medicationId + "', qty=" + quantity + "}";
    }
}