package hospital.management.backend.service.pharmacy;

import hospital.management.backend.dao.pharmacy.interfaces.MedicalInventoryDAO;
import hospital.management.backend.dao.pharmacy.interfaces.MedicationDAO;
import hospital.management.backend.dto.pharmacy.CreateMedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.CreateMedicationDTO;
import hospital.management.backend.dto.pharmacy.MedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.MedicationDTO;
import hospital.management.backend.service.pharmacy.interfaces.PharmacyService;

import java.util.List;

public class PharmacyServiceImpl implements PharmacyService {

    private final MedicationDAO       medicationDAO;
    private final MedicalInventoryDAO inventoryDAO;

    public PharmacyServiceImpl(MedicationDAO medicationDAO, MedicalInventoryDAO inventoryDAO) {
        this.medicationDAO = medicationDAO;
        this.inventoryDAO  = inventoryDAO;
    }

    @Override
    public MedicationDTO addMedication(CreateMedicationDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public MedicationDTO findMedicationById(String medicationId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<MedicationDTO> findAllMedications() throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public MedicalInventoryDTO addStock(CreateMedicalInventoryDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<MedicalInventoryDTO> findStockByMedication(String medicationId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<MedicalInventoryDTO> findLowStock() throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}