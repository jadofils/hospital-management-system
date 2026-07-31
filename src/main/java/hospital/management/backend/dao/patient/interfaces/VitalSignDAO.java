package hospital.management.backend.dao.patient.interfaces;

import hospital.management.backend.model.patient.VitalSign;

import java.util.List;
import java.util.Optional;

public interface VitalSignDAO {
    VitalSign save(VitalSign vitalSign) throws Exception;
    Optional<VitalSign> findById(String vitalId) throws Exception;
    Optional<VitalSign> findByAppointmentId(String appointmentId) throws Exception;
    List<VitalSign> findByPatientId(String patientId) throws Exception;
    void softDelete(String vitalId) throws Exception;
}