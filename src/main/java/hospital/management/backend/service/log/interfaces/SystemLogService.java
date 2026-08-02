package hospital.management.backend.service.log.interfaces;

import hospital.management.backend.dto.log.SystemLogDTO;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;

public interface SystemLogService {
    SystemLogDTO log(String level, String source, String message, String userId) throws Exception;
    PageResult<SystemLogDTO> findAll(PageRequest request) throws Exception;
    List<SystemLogDTO> findByLevel(String level) throws Exception;
    int purgeOlderThanDays(int days) throws Exception;
}