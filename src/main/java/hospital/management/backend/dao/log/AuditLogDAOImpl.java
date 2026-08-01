package hospital.management.backend.dao.log;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.log.interfaces.AuditLogDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.model.user.AuditLog;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** JDBC implementation against the append-only `audit_log` table (see hospital_schema.sql). */
public class AuditLogDAOImpl implements AuditLogDAO {

    private static final String SELECT_COLUMNS = "log_id, user_id, action, table_affected, record_id, created_at";

    @Override
    public AuditLog save(AuditLog log) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return save(log, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save audit log: " + e.getMessage(), e);
        }
    }

    @Override
    public AuditLog save(AuditLog log, Connection conn) throws Exception {
        UUID id = log.getLogId() != null ? UUID.fromString(log.getLogId()) : UUID.randomUUID();
        String sql = "INSERT INTO audit_log (log_id, user_id, action, table_affected, record_id) "
                   + "VALUES (?, ?, ?, ?, ?) RETURNING created_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            if (log.getUserId() != null) {
                ps.setObject(2, UUID.fromString(log.getUserId()));
            } else {
                ps.setNull(2, Types.OTHER);
            }
            ps.setString(3, log.getAction());
            ps.setString(4, log.getTableAffected());
            if (log.getRecordId() != null) {
                ps.setObject(5, UUID.fromString(log.getRecordId()));
            } else {
                ps.setNull(5, Types.OTHER);
            }
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
    public PageResult<AuditLog> findAll(PageRequest request) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM audit_log WHERE TRUE "
                   + CursorPagination.whereClause(request, "created_at")
                   + CursorPagination.orderClause(request, "created_at")
                   + "LIMIT ?";
        List<AuditLog> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, request.getPageSize() + 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list audit log: " + e.getMessage(), e);
        }
        return CursorPagination.toResult(rows, request, AuditLog::getCreatedAt);
    }

    @Override
    public List<AuditLog> findByUserId(String userId) throws Exception {
        return findWhere("user_id = ?", UUID.fromString(userId));
    }

    @Override
    public List<AuditLog> findByTable(String tableName) throws Exception {
        return findWhere("table_affected = ?", tableName);
    }

    private List<AuditLog> findWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM audit_log WHERE " + predicate + " ORDER BY created_at DESC";
        List<AuditLog> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up audit log: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public int deleteOlderThanDays(int days) throws Exception {
        String sql = "DELETE FROM audit_log WHERE created_at < CURRENT_TIMESTAMP - (? || ' days')::interval";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to purge old audit log entries: " + e.getMessage(), e);
        }
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setLogId(rs.getObject("log_id", UUID.class).toString());
        UUID userId = rs.getObject("user_id", UUID.class);
        log.setUserId(userId != null ? userId.toString() : null);
        log.setAction(rs.getString("action"));
        log.setTableAffected(rs.getString("table_affected"));
        UUID recordId = rs.getObject("record_id", UUID.class);
        log.setRecordId(recordId != null ? recordId.toString() : null);
        log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return log;
    }
}
