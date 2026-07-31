package hospital.management.backend.dao.log;

import hospital.management.backend.dao.log.interfaces.AuditLogDAO;
import hospital.management.backend.model.user.AuditLog;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;

public class AuditLogDAOImpl implements AuditLogDAO {

    @Override
    public AuditLog save(AuditLog log) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<AuditLog> findAll(PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<AuditLog> findByUserId(String userId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<AuditLog> findByTable(String tableName) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int deleteOlderThanDays(int days) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}