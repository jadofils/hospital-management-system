package hospital.management.backend.service.patient;

import hospital.management.backend.dao.patient.interfaces.PatientDAO;
import hospital.management.backend.dto.patient.CreatePatientDTO;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.patient.PatientSummaryDTO;
import hospital.management.backend.dto.patient.UpdatePatientDTO;
import hospital.management.backend.service.patient.interfaces.PatientService;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

public class PatientServiceImpl implements PatientService {

    private final PatientDAO patientDAO;

    public PatientServiceImpl(PatientDAO patientDAO) {
        this.patientDAO = patientDAO;
    }

    @Override
    public PatientDTO create(CreatePatientDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PatientDTO findById(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<PatientDTO> findAll(PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<PatientSummaryDTO> search(String query, PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PatientDTO update(UpdatePatientDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}