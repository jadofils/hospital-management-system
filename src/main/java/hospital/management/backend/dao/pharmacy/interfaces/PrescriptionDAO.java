package hospital.management.backend.dao.pharmacy.interfaces;

import hospital.management.backend.model.pharmacy.Prescription;

import java.util.List;
import java.util.Optional;

public interface PrescriptionDAO {
    Prescription save(Prescription prescription) throws Exception;
    Optional<Prescription> findById(String prescriptionId) throws Exception;
    Optional<Prescription> findByAppointmentId(String appointmentId) throws Exception;
    List<Prescription> findByPatientId(String patientId) throws Exception;
    void softDelete(String prescriptionId) throws Exception;
}