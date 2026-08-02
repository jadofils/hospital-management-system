package hospital.management.backend.dto.pharmacy;

import java.time.LocalDate;

public class CreateMedicalInventoryDTO {

    private String    medicationId;
    private String    batchNumber;
    private LocalDate expiryDate;
    private Integer   quantityInStock;
    private Integer   reorderLevel;
    private String    supplier;

    public CreateMedicalInventoryDTO() {}

    public CreateMedicalInventoryDTO(String medicationId, String batchNumber, LocalDate expiryDate,
                                     Integer quantityInStock, Integer reorderLevel, String supplier) {
        this.medicationId    = medicationId;
        this.batchNumber     = batchNumber;
        this.expiryDate      = expiryDate;
        this.quantityInStock = quantityInStock;
        this.reorderLevel    = reorderLevel;
        this.supplier        = supplier;
    }

    public String getMedicationId() { return medicationId; }
    public void setMedicationId(String medicationId) { this.medicationId = medicationId; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public Integer getQuantityInStock() { return quantityInStock; }
    public void setQuantityInStock(Integer quantityInStock) { this.quantityInStock = quantityInStock; }

    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    @Override
    public String toString() {
        return "CreateMedicalInventoryDTO{medicationId='" + medicationId + "'}";
    }
}