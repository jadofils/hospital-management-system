package hospital.management.backend.service.pharmacy;

import hospital.management.backend.dao.pharmacy.interfaces.PrescriptionDAO;
import hospital.management.backend.dao.pharmacy.interfaces.PrescriptionItemDAO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import hospital.management.backend.service.pharmacy.interfaces.PrescriptionService;

import java.util.List;

public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionDAO     prescriptionDAO;
    private final PrescriptionItemDAO itemDAO;

    public PrescriptionServiceImpl(PrescriptionDAO prescriptionDAO, PrescriptionItemDAO itemDAO) {
        this.prescriptionDAO = prescriptionDAO;
        this.itemDAO         = itemDAO;
    }

    @Override
    public PrescriptionDTO issue(CreatePrescriptionDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PrescriptionDTO findById(String prescriptionId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PrescriptionDTO findByAppointment(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<PrescriptionDTO> findByPatient(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String prescriptionId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}