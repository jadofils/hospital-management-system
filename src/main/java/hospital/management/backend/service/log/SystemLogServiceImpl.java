package hospital.management.backend.service.log;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.dao.log.interfaces.SystemLogDAO;
import hospital.management.backend.dto.log.SystemLogDTO;
import hospital.management.backend.mapper.log.SystemLogMapper;
import hospital.management.backend.model.user.SystemLog;
import hospital.management.backend.service.log.interfaces.SystemLogService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.ArrayList;
import java.util.List;

public class SystemLogServiceImpl implements SystemLogService {

    private static final AppLogger logger = AppLogger.getLogger(SystemLogServiceImpl.class);

    private final SystemLogDAO systemLogDAO;

    public SystemLogServiceImpl(SystemLogDAO systemLogDAO) {
        this.systemLogDAO = systemLogDAO;
    }

    @Override
    public SystemLogDTO log(String level, String source, String message, String userId) throws Exception {
        ValidatorUtils.requireNonBlank(level, "level");
        ValidatorUtils.requireNonBlank(source, "source");
        ValidatorUtils.requireNonBlank(message, "message");

        SystemLog entry = new SystemLog();
        entry.setLogLevel(level);
        entry.setSource(source);
        entry.setMessage(message);
        entry.setUserId(userId);
        entry.setCreatedAt(java.time.LocalDateTime.now());

        SystemLog saved = systemLogDAO.save(entry);
        logger.info("System log recorded: [" + saved.getLogLevel() + "] " + saved.getSource());
        EventBus.publish(AppEventType.SYSTEM_LOG_RECORDED, saved.getLogId());
        return SystemLogMapper.toDTO(saved);
    }

    @Override
    public PageResult<SystemLogDTO> findAll(PageRequest request) throws Exception {
        return systemLogDAO.findAll(request).map(SystemLogMapper::toDTO);
    }

    @Override
    public List<SystemLogDTO> findByLevel(String level) throws Exception {
        List<SystemLogDTO> dtos = new ArrayList<>();
        for (SystemLog log : systemLogDAO.findByLevel(level)) dtos.add(SystemLogMapper.toDTO(log));
        return dtos;
    }

    @Override
    public int purgeOlderThanDays(int days) throws Exception {
        return systemLogDAO.deleteOlderThanDays(days);
    }
}
