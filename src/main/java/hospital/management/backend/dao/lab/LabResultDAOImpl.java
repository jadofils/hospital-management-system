package hospital.management.backend.dao.lab;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.lab.interfaces.LabResultDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.lab.LabResult;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `lab_results` table (see hospital_schema.sql). */
public class LabResultDAOImpl implements LabResultDAO {

    private static final String SELECT_COLUMNS =
        "lab_result_id, lab_order_id, result_value, unit, reference_range, is_abnormal, " +
        "completed_at, created_at, updated_at, deleted_at";

    @Override
    public LabResult save(LabResult result) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return save(result, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save lab result: " + e.getMessage(), e);
        }
    }

    @Override
    public LabResult save(LabResult result, Connection conn) throws Exception {
        UUID id = result.getLabResultId() != null ? UUID.fromString(result.getLabResultId()) : UUID.randomUUID();
        String sql = "INSERT INTO lab_results (lab_result_id, lab_order_id, result_value, unit, reference_range, is_abnormal, completed_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING created_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(result.getLabOrderId()));
            ps.setString(3, result.getResultValue());
            ps.setString(4, result.getUnit());
            ps.setString(5, result.getReferenceRange());
            ps.setBoolean(6, result.isIsAbnormal() != null ? result.isIsAbnormal() : false);
            if (result.getCompletedAt() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(result.getCompletedAt()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.setLabResultId(id.toString());
                    result.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    result.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return result;
    }

    @Override
    public Optional<LabResult> findById(String labResultId) throws Exception {
        return findOneWhere("lab_result_id = ?", UUID.fromString(labResultId));
    }

    @Override
    public Optional<LabResult> findByLabOrderId(String labOrderId) throws Exception {
        return findOneWhere("lab_order_id = ?", UUID.fromString(labOrderId));
    }

    private Optional<LabResult> findOneWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM lab_results WHERE " + predicate + " AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up lab result: " + e.getMessage(), e);
        }
    }

    @Override
    public void softDelete(String labResultId) throws Exception {
        String sql = "UPDATE lab_results SET deleted_at = CURRENT_TIMESTAMP WHERE lab_result_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(labResultId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("LabResult", labResultId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete lab result: " + e.getMessage(), e);
        }
    }

    private LabResult mapRow(ResultSet rs) throws SQLException {
        LabResult r = new LabResult();
        r.setLabResultId(rs.getObject("lab_result_id", UUID.class).toString());
        r.setLabOrderId(rs.getObject("lab_order_id", UUID.class).toString());
        r.setResultValue(rs.getString("result_value"));
        r.setUnit(rs.getString("unit"));
        r.setReferenceRange(rs.getString("reference_range"));
        r.setIsAbnormal(rs.getBoolean("is_abnormal"));
        Timestamp completedAt = rs.getTimestamp("completed_at");
        r.setCompletedAt(completedAt != null ? completedAt.toLocalDateTime() : null);
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        r.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        r.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return r;
    }
}
