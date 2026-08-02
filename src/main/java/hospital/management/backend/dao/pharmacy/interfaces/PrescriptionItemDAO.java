package hospital.management.backend.dao.pharmacy.interfaces;

import hospital.management.backend.model.pharmacy.PrescriptionItem;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface PrescriptionItemDAO {
    PrescriptionItem save(PrescriptionItem item) throws Exception;

    /** Same as {@link #save(PrescriptionItem)} but runs on a caller-supplied connection, so a
     *  service can compose it into a larger transaction (e.g. prescription header + line items). */
    PrescriptionItem save(PrescriptionItem item, Connection conn) throws Exception;
    Optional<PrescriptionItem> findById(String itemId) throws Exception;
    List<PrescriptionItem> findByPrescriptionId(String prescriptionId) throws Exception;
    void softDelete(String itemId) throws Exception;
}