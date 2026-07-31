package hospital.management.backend.dao.patient;

import hospital.management.backend.dao.patient.interfaces.VitalSignDAO;
import hospital.management.backend.model.patient.VitalSign;

import java.util.List;
import java.util.Optional;

public class VitalSignDAOImpl implements VitalSignDAO {

    @Override
    public VitalSign save(VitalSign vitalSign) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<VitalSign> findById(String vitalId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<VitalSign> findByAppointmentId(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<VitalSign> findByPatientId(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String vitalId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}