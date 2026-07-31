package hospital.management.backend.mapper.log;

import hospital.management.backend.dto.log.AuditLogDTO;
import hospital.management.backend.model.user.AuditLog;

public class AuditLogMapper {

    public static AuditLogDTO toDTO(AuditLog log) {
        if (log == null) return null;
        return new AuditLogDTO(
            log.getId(),
            log.getUserId(),
            log.getAction(),
            log.getTableAffected(),
            log.getRecordId(),
            log.getCreatedAt()
        );
    }

    public static AuditLog toEntity(AuditLogDTO dto) {
        if (dto == null) return null;
        AuditLog log = new AuditLog();
        log.setLogId(dto.getLogId());
        log.setUserId(dto.getUserId());
        log.setAction(dto.getAction());
        log.setTableAffected(dto.getTableAffected());
        log.setRecordId(dto.getRecordId());
        return log;
    }
}