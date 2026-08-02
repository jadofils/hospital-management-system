package hospital.management.backend.dao.pharmacy.interfaces;

import hospital.management.backend.model.pharmacy.MedicalInventory;

import java.util.List;
import java.util.Optional;

public interface MedicalInventoryDAO {
    MedicalInventory save(MedicalInventory inventory) throws Exception;
    Optional<MedicalInventory> findById(String inventoryId) throws Exception;
    List<MedicalInventory> findByMedicationId(String medicationId) throws Exception;
    List<MedicalInventory> findLowStock() throws Exception;
    MedicalInventory update(MedicalInventory inventory) throws Exception;
    void softDelete(String inventoryId) throws Exception;
}