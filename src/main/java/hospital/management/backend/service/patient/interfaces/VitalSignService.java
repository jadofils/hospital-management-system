package hospital.management.backend.service.patient.interfaces;

import hospital.management.backend.dto.patient.CreateVitalSignDTO;
import hospital.management.backend.dto.patient.VitalSignDTO;

import java.util.List;

public interface VitalSignService {
    VitalSignDTO record(CreateVitalSignDTO dto) throws Exception;
    VitalSignDTO findByAppointment(String appointmentId) throws Exception;
    List<VitalSignDTO> findByPatient(String patientId) throws Exception;
    void delete(String vitalId) throws Exception;
}