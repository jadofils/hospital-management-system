package hospital.management.backend.dao.lab;

import hospital.management.backend.dao.lab.interfaces.LabResultDAO;
import hospital.management.backend.model.lab.LabResult;

import java.util.Optional;

public class LabResultDAOImpl implements LabResultDAO {

    @Override
    public LabResult save(LabResult result) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<LabResult> findById(String labResultId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<LabResult> findByLabOrderId(String labOrderId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String labResultId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}