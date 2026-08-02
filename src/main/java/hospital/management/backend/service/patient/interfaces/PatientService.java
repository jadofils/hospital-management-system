package hospital.management.backend.service.patient.interfaces;

import hospital.management.backend.dto.patient.CreatePatientDTO;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.patient.PatientSummaryDTO;
import hospital.management.backend.dto.patient.UpdatePatientDTO;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

public interface PatientService {
    PatientDTO create(CreatePatientDTO dto) throws Exception;
    PatientDTO findById(String patientId) throws Exception;
    PageResult<PatientDTO> findAll(PageRequest request) throws Exception;
    PageResult<PatientSummaryDTO> search(String query, PageRequest request) throws Exception;
    PatientDTO update(UpdatePatientDTO dto) throws Exception;
    void delete(String patientId) throws Exception;
}