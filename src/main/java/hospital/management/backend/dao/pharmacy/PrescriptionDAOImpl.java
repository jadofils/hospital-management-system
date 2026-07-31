package hospital.management.backend.dao.pharmacy;

import hospital.management.backend.dao.pharmacy.interfaces.PrescriptionDAO;
import hospital.management.backend.model.pharmacy.Prescription;

import java.util.List;
import java.util.Optional;

public class PrescriptionDAOImpl implements PrescriptionDAO {

    @Override
    public Prescription save(Prescription prescription) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Prescription> findById(String prescriptionId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Prescription> findByAppointmentId(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Prescription> findByPatientId(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String prescriptionId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}