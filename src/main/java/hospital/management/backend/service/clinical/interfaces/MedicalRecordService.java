package hospital.management.backend.service.clinical.interfaces;

import hospital.management.backend.dto.clinical.CreateMedicalRecordDTO;
import hospital.management.backend.dto.clinical.MedicalRecordDTO;

public interface MedicalRecordService {
    MedicalRecordDTO create(CreateMedicalRecordDTO dto) throws Exception;
    MedicalRecordDTO findById(String recordId) throws Exception;
    MedicalRecordDTO findByAppointment(String appointmentId) throws Exception;
    MedicalRecordDTO update(String recordId, CreateMedicalRecordDTO dto) throws Exception;
    void delete(String recordId) throws Exception;
}