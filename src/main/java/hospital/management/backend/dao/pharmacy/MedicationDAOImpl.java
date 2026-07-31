package hospital.management.backend.dao.pharmacy;

import hospital.management.backend.dao.pharmacy.interfaces.MedicationDAO;
import hospital.management.backend.model.pharmacy.Medication;

import java.util.List;
import java.util.Optional;

public class MedicationDAOImpl implements MedicationDAO {

    @Override
    public Medication save(Medication medication) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Medication> findById(String medicationId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Medication> findByName(String name) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Medication> findAll() throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Medication update(Medication medication) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String medicationId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}