package hospital.management.backend.dao.pharmacy;

import hospital.management.backend.dao.pharmacy.interfaces.MedicalInventoryDAO;
import hospital.management.backend.model.pharmacy.MedicalInventory;

import java.util.List;
import java.util.Optional;

public class MedicalInventoryDAOImpl implements MedicalInventoryDAO {

    @Override
    public MedicalInventory save(MedicalInventory inventory) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<MedicalInventory> findById(String inventoryId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<MedicalInventory> findByMedicationId(String medicationId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<MedicalInventory> findLowStock() throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public MedicalInventory update(MedicalInventory inventory) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String inventoryId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}