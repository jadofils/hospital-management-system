package hospital.management.backend.service.log;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.dao.log.interfaces.AuditLogDAO;
import hospital.management.backend.dto.log.AuditLogDTO;
import hospital.management.backend.mapper.log.AuditLogMapper;
import hospital.management.backend.model.user.AuditLog;
import hospital.management.backend.service.log.interfaces.AuditService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.ArrayList;
import java.util.List;

public class AuditServiceImpl implements AuditService {

    private static final AppLogger logger = AppLogger.getLogger(AuditServiceImpl.class);

    private final AuditLogDAO auditLogDAO;

    public AuditServiceImpl(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    @Override
    public AuditLogDTO record(String userId, String action, String table, String recordId) throws Exception {
        ValidatorUtils.requireNonBlank(action, "action");
        ValidatorUtils.requireNonBlank(table, "table");

        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTableAffected(table);
        log.setRecordId(recordId);

        AuditLog saved = auditLogDAO.save(log);
        logger.info("Audit recorded: " + saved.getAction() + " on " + saved.getTableAffected());
        EventBus.publish(AppEventType.AUDIT_LOG_RECORDED, saved.getLogId());
        return AuditLogMapper.toDTO(saved);
    }

    @Override
    public PageResult<AuditLogDTO> findAll(PageRequest request) throws Exception {
        return auditLogDAO.findAll(request).map(AuditLogMapper::toDTO);
    }

    @Override
    public List<AuditLogDTO> findByUser(String userId) throws Exception {
        List<AuditLogDTO> dtos = new ArrayList<>();
        for (AuditLog log : auditLogDAO.findByUserId(userId)) dtos.add(AuditLogMapper.toDTO(log));
        return dtos;
    }

    @Override
    public int purgeOlderThanDays(int days) throws Exception {
        return auditLogDAO.deleteOlderThanDays(days);
    }
}
