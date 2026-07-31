package hospital.management.backend.dao.log;

import hospital.management.backend.dao.log.interfaces.SystemLogDAO;
import hospital.management.backend.model.user.SystemLog;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;

public class SystemLogDAOImpl implements SystemLogDAO {

    @Override
    public SystemLog save(SystemLog log) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<SystemLog> findAll(PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<SystemLog> findByLevel(String logLevel) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<SystemLog> findBySource(String source) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int deleteOlderThanDays(int days) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}