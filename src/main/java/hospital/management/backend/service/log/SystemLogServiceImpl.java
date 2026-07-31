package hospital.management.backend.service.log;

import hospital.management.backend.dao.log.interfaces.SystemLogDAO;
import hospital.management.backend.dto.log.SystemLogDTO;
import hospital.management.backend.service.log.interfaces.SystemLogService;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;

public class SystemLogServiceImpl implements SystemLogService {

    private final SystemLogDAO systemLogDAO;

    public SystemLogServiceImpl(SystemLogDAO systemLogDAO) {
        this.systemLogDAO = systemLogDAO;
    }

    @Override
    public SystemLogDTO log(String level, String source, String message, String userId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<SystemLogDTO> findAll(PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<SystemLogDTO> findByLevel(String level) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int purgeOlderThanDays(int days) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}