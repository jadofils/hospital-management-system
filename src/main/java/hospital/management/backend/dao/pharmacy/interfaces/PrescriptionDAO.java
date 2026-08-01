package hospital.management.backend.dao.pharmacy.interfaces;

import hospital.management.backend.model.pharmacy.Prescription;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface PrescriptionDAO {
    Prescription save(Prescription prescription) throws Exception;

    /** Same as {@link #save(Prescription)} but runs on a caller-supplied connection, so a
     *  service can compose it into a larger transaction (e.g. prescription header + line items). */
    Prescription save(Prescription prescription, Connection conn) throws Exception;
    Optional<Prescription> findById(String prescriptionId) throws Exception;
    Optional<Prescription> findByAppointmentId(String appointmentId) throws Exception;
    List<Prescription> findByPatientId(String patientId) throws Exception;
    void softDelete(String prescriptionId) throws Exception;
}