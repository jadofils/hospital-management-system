package hospital.management.backend.dao.pharmacy.interfaces;

import hospital.management.backend.model.pharmacy.Medication;

import java.util.List;
import java.util.Optional;

public interface MedicationDAO {
    Medication save(Medication medication) throws Exception;
    Optional<Medication> findById(String medicationId) throws Exception;
    Optional<Medication> findByName(String name) throws Exception;
    List<Medication> findAll() throws Exception;
    Medication update(Medication medication) throws Exception;
    void softDelete(String medicationId) throws Exception;
}