package hospital.management.backend.service.log;

import hospital.management.backend.dao.log.interfaces.AuditLogDAO;
import hospital.management.backend.dto.log.AuditLogDTO;
import hospital.management.backend.service.log.interfaces.AuditService;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;

public class AuditServiceImpl implements AuditService {

    private final AuditLogDAO auditLogDAO;

    public AuditServiceImpl(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    @Override
    public AuditLogDTO record(String userId, String action, String table, String recordId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<AuditLogDTO> findAll(PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<AuditLogDTO> findByUser(String userId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int purgeOlderThanDays(int days) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}