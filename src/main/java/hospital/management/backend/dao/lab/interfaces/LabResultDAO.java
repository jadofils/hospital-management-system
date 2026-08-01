package hospital.management.backend.dao.lab.interfaces;

import hospital.management.backend.model.lab.LabResult;

import java.sql.Connection;
import java.util.Optional;

public interface LabResultDAO {
    LabResult save(LabResult result) throws Exception;

    /** Transaction-composable overload — participates in a caller-owned connection/commit. */
    LabResult save(LabResult result, Connection conn) throws Exception;

    Optional<LabResult> findById(String labResultId) throws Exception;
    Optional<LabResult> findByLabOrderId(String labOrderId) throws Exception;
    void softDelete(String labResultId) throws Exception;
}