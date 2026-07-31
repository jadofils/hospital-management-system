package hospital.management.backend.dao.pharmacy;

import hospital.management.backend.dao.pharmacy.interfaces.PrescriptionItemDAO;
import hospital.management.backend.model.pharmacy.PrescriptionItem;

import java.util.List;
import java.util.Optional;

public class PrescriptionItemDAOImpl implements PrescriptionItemDAO {

    @Override
    public PrescriptionItem save(PrescriptionItem item) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<PrescriptionItem> findById(String itemId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<PrescriptionItem> findByPrescriptionId(String prescriptionId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String itemId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}