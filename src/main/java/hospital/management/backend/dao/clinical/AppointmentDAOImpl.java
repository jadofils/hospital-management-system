package hospital.management.backend.dao.clinical;

import hospital.management.backend.dao.clinical.interfaces.AppointmentDAO;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;
import java.util.Optional;

public class AppointmentDAOImpl implements AppointmentDAO {

    @Override
    public Appointment save(Appointment appointment) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Appointment> findById(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<Appointment> findAll(PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Appointment> findByPatientId(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Appointment> findByDoctorId(String doctorId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Appointment update(Appointment appointment) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}