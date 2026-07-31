package hospital.management.backend.dao.clinical;

import hospital.management.backend.dao.clinical.interfaces.MedicalRecordDAO;
import hospital.management.backend.model.patient.MedicalRecord;

import java.util.Optional;

public class MedicalRecordDAOImpl implements MedicalRecordDAO {

    @Override
    public MedicalRecord save(MedicalRecord record) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<MedicalRecord> findById(String recordId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<MedicalRecord> findByAppointmentId(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public MedicalRecord update(MedicalRecord record) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String recordId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}