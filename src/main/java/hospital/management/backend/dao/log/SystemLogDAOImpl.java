package hospital.management.backend.dao.log;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.log.interfaces.SystemLogDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.model.user.SystemLog;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** JDBC implementation against the append-only `system_logs` table (see hospital_schema.sql). */
public class SystemLogDAOImpl implements SystemLogDAO {

    private static final String SELECT_COLUMNS = "log_id, user_id, log_level, source, message, created_at";

    @Override
    public SystemLog save(SystemLog log) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return save(log, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save system log: " + e.getMessage(), e);
        }
    }

    @Override
    public SystemLog save(SystemLog log, Connection conn) throws Exception {
        UUID id = log.getLogId() != null ? UUID.fromString(log.getLogId()) : UUID.randomUUID();
        String sql = "INSERT INTO system_logs (log_id, user_id, log_level, source, message) "
                   + "VALUES (?, ?, ?, ?, ?) RETURNING created_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            if (log.getUserId() != null) {
                ps.setObject(2, UUID.fromString(log.getUserId()));
            } else {
                ps.setNull(2, Types.OTHER);
            }
            ps.setString(3, log.getLogLevel());
            ps.setString(4, log.getSource());
            ps.setString(5, log.getMessage());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    log.setLogId(id.toString());
                    log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
        }
        return log;
    }

    @Override
    public PageResult<SystemLog> findAll(PageRequest request) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM system_logs WHERE TRUE "
                   + CursorPagination.whereClause(request, "created_at")
                   + CursorPagination.orderClause(request, "created_at")
                   + "LIMIT ?";
        List<SystemLog> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, request.getPageSize() + 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list system logs: " + e.getMessage(), e);
        }
        return CursorPagination.toResult(rows, request, SystemLog::getCreatedAt);
    }

    @Override
    public List<SystemLog> findByLevel(String logLevel) throws Exception {
        return findWhere("log_level = ?", logLevel);
    }

    @Override
    public List<SystemLog> findBySource(String source) throws Exception {
        return findWhere("source = ?", source);
    }

    private List<SystemLog> findWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM system_logs WHERE " + predicate + " ORDER BY created_at DESC";
        List<SystemLog> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up system logs: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public int deleteOlderThanDays(int days) throws Exception {
        String sql = "DELETE FROM system_logs WHERE created_at < CURRENT_TIMESTAMP - (? || ' days')::interval";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to purge old system log entries: " + e.getMessage(), e);
        }
    }

    private SystemLog mapRow(ResultSet rs) throws SQLException {
        SystemLog log = new SystemLog();
        log.setLogId(rs.getObject("log_id", UUID.class).toString());
        UUID userId = rs.getObject("user_id", UUID.class);
        log.setUserId(userId != null ? userId.toString() : null);
        log.setLogLevel(rs.getString("log_level"));
        log.setSource(rs.getString("source"));
        log.setMessage(rs.getString("message"));
        log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return log;
    }
}
