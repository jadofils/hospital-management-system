package hospital.management.backend.mapper.pharmacy;

import hospital.management.backend.dto.pharmacy.CreateMedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.MedicalInventoryDTO;
import hospital.management.backend.model.pharmacy.MedicalInventory;

public class MedicalInventoryMapper {

    public static MedicalInventoryDTO toDTO(MedicalInventory i) {
        if (i == null) return null;
        return new MedicalInventoryDTO(
            i.getInventoryId(),
            i.getMedicationId(),
            i.getBatchNumber(),
            i.getExpiryDate(),
            i.getQuantityInStock(),
            i.getReorderLevel(),
            i.getSupplier(),
            i.getCreatedAt()
        );
    }

    public static MedicalInventory toEntity(CreateMedicalInventoryDTO dto) {
        if (dto == null) return null;
        MedicalInventory i = new MedicalInventory();
        i.setMedicationId(dto.getMedicationId());
        i.setBatchNumber(dto.getBatchNumber());
        i.setExpiryDate(dto.getExpiryDate());
        i.setQuantityInStock(dto.getQuantityInStock());
        i.setReorderLevel(dto.getReorderLevel());
        i.setSupplier(dto.getSupplier());
        return i;
    }
}