package hospital.management.backend.dao.log.interfaces;

import hospital.management.backend.model.user.SystemLog;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.sql.Connection;
import java.util.List;

public interface SystemLogDAO {
    SystemLog save(SystemLog log) throws Exception;

    /** Same as {@link #save(SystemLog)} but composable into a caller-managed transaction. */
    SystemLog save(SystemLog log, Connection conn) throws Exception;
    PageResult<SystemLog> findAll(PageRequest request) throws Exception;
    List<SystemLog> findByLevel(String logLevel) throws Exception;
    List<SystemLog> findBySource(String source) throws Exception;
    int deleteOlderThanDays(int days) throws Exception;
}