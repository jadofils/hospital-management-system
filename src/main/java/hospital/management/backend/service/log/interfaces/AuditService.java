package hospital.management.backend.service.log.interfaces;

import hospital.management.backend.dto.log.AuditLogDTO;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;

public interface AuditService {
    AuditLogDTO record(String userId, String action, String table, String recordId) throws Exception;
    PageResult<AuditLogDTO> findAll(PageRequest request) throws Exception;
    List<AuditLogDTO> findByUser(String userId) throws Exception;
    int purgeOlderThanDays(int days) throws Exception;
}