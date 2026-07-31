package hospital.management.backend.dao.log.interfaces;

import hospital.management.backend.model.user.AuditLog;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;

public interface AuditLogDAO {
    AuditLog save(AuditLog log) throws Exception;
    PageResult<AuditLog> findAll(PageRequest request) throws Exception;
    List<AuditLog> findByUserId(String userId) throws Exception;
    List<AuditLog> findByTable(String tableName) throws Exception;
    int deleteOlderThanDays(int days) throws Exception;
}