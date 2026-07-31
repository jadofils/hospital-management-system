package hospital.management.backend.mapper.log;

import hospital.management.backend.dto.log.SystemLogDTO;
import hospital.management.backend.model.user.SystemLog;

public class SystemLogMapper {

    public static SystemLogDTO toDTO(SystemLog log) {
        if (log == null) return null;
        return new SystemLogDTO(
            log.getId(),
            log.getLogLevel(),
            log.getSource(),
            log.getMessage(),
            log.getUserId(),
            log.getCreatedAt()
        );
    }

    public static SystemLog toEntity(SystemLogDTO dto) {
        if (dto == null) return null;
        SystemLog log = new SystemLog();
        log.setLogId(dto.getLogId());
        log.setLogLevel(dto.getLogLevel());
        log.setSource(dto.getSource());
        log.setMessage(dto.getMessage());
        log.setUserId(dto.getUserId());
        return log;
    }
}