package hospital.management.backend.service.patient;

import hospital.management.backend.dao.patient.interfaces.VitalSignDAO;
import hospital.management.backend.dto.patient.CreateVitalSignDTO;
import hospital.management.backend.dto.patient.VitalSignDTO;
import hospital.management.backend.service.patient.interfaces.VitalSignService;

import java.util.List;

public class VitalSignServiceImpl implements VitalSignService {

    private final VitalSignDAO vitalSignDAO;

    public VitalSignServiceImpl(VitalSignDAO vitalSignDAO) {
        this.vitalSignDAO = vitalSignDAO;
    }

    @Override
    public VitalSignDTO record(CreateVitalSignDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public VitalSignDTO findByAppointment(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<VitalSignDTO> findByPatient(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String vitalId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}