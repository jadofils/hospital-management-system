package hospital.management.backend.dao.lab;

import hospital.management.backend.dao.lab.interfaces.LabOrderDAO;
import hospital.management.backend.model.lab.LabOrder;

import java.util.List;
import java.util.Optional;

public class LabOrderDAOImpl implements LabOrderDAO {

    @Override
    public LabOrder save(LabOrder order) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<LabOrder> findById(String labOrderId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<LabOrder> findByAppointmentId(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<LabOrder> findByDoctorId(String doctorId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public LabOrder updateStatus(String labOrderId, String status) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String labOrderId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}