package hospital.management.backend.dao.pharmacy.interfaces;

import hospital.management.backend.model.pharmacy.PrescriptionItem;

import java.util.List;
import java.util.Optional;

public interface PrescriptionItemDAO {
    PrescriptionItem save(PrescriptionItem item) throws Exception;
    Optional<PrescriptionItem> findById(String itemId) throws Exception;
    List<PrescriptionItem> findByPrescriptionId(String prescriptionId) throws Exception;
    void softDelete(String itemId) throws Exception;
}