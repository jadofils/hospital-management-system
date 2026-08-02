package hospital.management.backend.service.pharmacy.interfaces;

import hospital.management.backend.dto.pharmacy.CreateMedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.CreateMedicationDTO;
import hospital.management.backend.dto.pharmacy.MedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.MedicationDTO;

import java.util.List;

public interface PharmacyService {
    MedicationDTO addMedication(CreateMedicationDTO dto) throws Exception;
    MedicationDTO findMedicationById(String medicationId) throws Exception;
    List<MedicationDTO> findAllMedications() throws Exception;
    MedicalInventoryDTO addStock(CreateMedicalInventoryDTO dto) throws Exception;
    List<MedicalInventoryDTO> findStockByMedication(String medicationId) throws Exception;
    List<MedicalInventoryDTO> findLowStock() throws Exception;

    /**
     * Updates an existing inventory batch (quantity, reorder level, supplier, etc.).
     * Not part of the original interface sketch, but required so the low-stock
     * transition logic (quantity drops to/below reorder level on a stock update)
     * has somewhere real to live — see {@code PharmacyServiceImpl} for the
     * before/after comparison that decides whether {@code INVENTORY_LOW_STOCK} fires.
     */
    MedicalInventoryDTO updateStock(String inventoryId, CreateMedicalInventoryDTO dto) throws Exception;
}