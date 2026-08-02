package hospital.management.backend.service.pharmacy.interfaces;

import hospital.management.backend.dto.pharmacy.CreatePrescriptionDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;

import java.util.List;

public interface PrescriptionService {
    PrescriptionDTO issue(CreatePrescriptionDTO dto) throws Exception;
    PrescriptionDTO findById(String prescriptionId) throws Exception;
    PrescriptionDTO findByAppointment(String appointmentId) throws Exception;
    List<PrescriptionDTO> findByPatient(String patientId) throws Exception;
    void delete(String prescriptionId) throws Exception;
}