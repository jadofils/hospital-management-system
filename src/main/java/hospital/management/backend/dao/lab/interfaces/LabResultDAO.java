package hospital.management.backend.dao.lab.interfaces;

import hospital.management.backend.model.lab.LabResult;

import java.util.Optional;

public interface LabResultDAO {
    LabResult save(LabResult result) throws Exception;
    Optional<LabResult> findById(String labResultId) throws Exception;
    Optional<LabResult> findByLabOrderId(String labOrderId) throws Exception;
    void softDelete(String labResultId) throws Exception;
}