package hospital.management.backend.dto.pharmacy;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MedicalInventoryDTO {

    private String        inventoryId;
    private String        medicationId;
    private String        batchNumber;
    private LocalDate     expiryDate;
    private Integer       quantityInStock;
    private Integer       reorderLevel;
    private String        supplier;
    private LocalDateTime createdAt;

    public MedicalInventoryDTO() {}

    public MedicalInventoryDTO(String inventoryId, String medicationId, String batchNumber,
                               LocalDate expiryDate, Integer quantityInStock, Integer reorderLevel,
                               String supplier, LocalDateTime createdAt) {
        this.inventoryId     = inventoryId;
        this.medicationId    = medicationId;
        this.batchNumber     = batchNumber;
        this.expiryDate      = expiryDate;
        this.quantityInStock = quantityInStock;
        this.reorderLevel    = reorderLevel;
        this.supplier        = supplier;
        this.createdAt       = createdAt;
    }

    public String getInventoryId() { return inventoryId; }
    public void setInventoryId(String inventoryId) { this.inventoryId = inventoryId; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStockAlert() {
        if (quantityInStock == null || quantityInStock <= 0) return "Out of Stock";
        if (reorderLevel != null && quantityInStock <= reorderLevel) {
            if (reorderLevel > 0 && quantityInStock <= reorderLevel / 2) return "Critical";
            return "Low";
        }
        return "OK";
    }

    @Override
    public String toString() {
        return "MedicalInventoryDTO{inventoryId='" + inventoryId + "', medicationId='" + medicationId + "'}";
    }
}