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
}