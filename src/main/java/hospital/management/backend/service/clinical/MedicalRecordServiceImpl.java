package hospital.management.backend.service.clinical;

import hospital.management.backend.dao.clinical.interfaces.MedicalRecordDAO;
import hospital.management.backend.dto.clinical.CreateMedicalRecordDTO;
import hospital.management.backend.dto.clinical.MedicalRecordDTO;
import hospital.management.backend.service.clinical.interfaces.MedicalRecordService;

public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordDAO recordDAO;

    public MedicalRecordServiceImpl(MedicalRecordDAO recordDAO) {
        this.recordDAO = recordDAO;
    }

    @Override
    public MedicalRecordDTO create(CreateMedicalRecordDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public MedicalRecordDTO findById(String recordId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public MedicalRecordDTO findByAppointment(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public MedicalRecordDTO update(String recordId, CreateMedicalRecordDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String recordId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}