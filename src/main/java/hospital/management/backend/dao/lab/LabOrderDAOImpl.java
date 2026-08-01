package hospital.management.backend.dao.lab;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.lab.interfaces.LabOrderDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.lab.LabOrder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `lab_orders` table (see hospital_schema.sql). */
public class LabOrderDAOImpl implements LabOrderDAO {

    private static final String SELECT_COLUMNS =
        "lab_order_id, appointment_id, doctor_id, test_name, status, ordered_at, updated_at, deleted_at";

    @Override
    public LabOrder save(LabOrder order) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return save(order, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save lab order: " + e.getMessage(), e);
        }
    }

    @Override
    public LabOrder save(LabOrder order, Connection conn) throws Exception {
        UUID id = order.getLabOrderId() != null ? UUID.fromString(order.getLabOrderId()) : UUID.randomUUID();
        String sql = "INSERT INTO lab_orders (lab_order_id, appointment_id, doctor_id, test_name, status) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING ordered_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(order.getAppointmentId()));
            ps.setObject(3, UUID.fromString(order.getDoctorId()));
            ps.setString(4, order.getTestName());
            ps.setString(5, order.getStatus() != null ? order.getStatus() : "ordered");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    order.setLabOrderId(id.toString());
                    order.setOrderedAt(rs.getTimestamp("ordered_at").toLocalDateTime());
                    order.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return order;
    }

    @Override
    public Optional<LabOrder> findById(String labOrderId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM lab_orders WHERE lab_order_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(labOrderId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up lab order: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LabOrder> findByAppointmentId(String appointmentId) throws Exception {
        return findAllWhere("appointment_id = ?", UUID.fromString(appointmentId));
    }

    @Override
    public List<LabOrder> findByDoctorId(String doctorId) throws Exception {
        return findAllWhere("doctor_id = ?", UUID.fromString(doctorId));
    }

    private List<LabOrder> findAllWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM lab_orders WHERE " + predicate
                   + " AND deleted_at IS NULL ORDER BY ordered_at DESC";
        List<LabOrder> orders = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) orders.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list lab orders: " + e.getMessage(), e);
        }
        return orders;
    }

    @Override
    public LabOrder updateStatus(String labOrderId, String status) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            return updateStatus(labOrderId, status, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update lab order status: " + e.getMessage(), e);
        }
    }

    @Override
    public LabOrder updateStatus(String labOrderId, String status, Connection conn) throws Exception {
        String sql = "UPDATE lab_orders SET status = ? WHERE lab_order_id = ? AND deleted_at IS NULL "
                   + "RETURNING appointment_id, doctor_id, test_name, ordered_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setObject(2, UUID.fromString(labOrderId));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ResourceNotFoundException("LabOrder", labOrderId);
                LabOrder order = new LabOrder();
                order.setLabOrderId(labOrderId);
                order.setAppointmentId(rs.getObject("appointment_id", UUID.class).toString());
                order.setDoctorId(rs.getObject("doctor_id", UUID.class).toString());
                order.setTestName(rs.getString("test_name"));
                order.setStatus(status);
                order.setOrderedAt(rs.getTimestamp("ordered_at").toLocalDateTime());
                order.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                return order;
            }
        }
    }

    @Override
    public void softDelete(String labOrderId) throws Exception {
        String sql = "UPDATE lab_orders SET deleted_at = CURRENT_TIMESTAMP WHERE lab_order_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(labOrderId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("LabOrder", labOrderId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete lab order: " + e.getMessage(), e);
        }
    }

    private LabOrder mapRow(ResultSet rs) throws SQLException {
        LabOrder o = new LabOrder();
        o.setLabOrderId(rs.getObject("lab_order_id", UUID.class).toString());
        o.setAppointmentId(rs.getObject("appointment_id", UUID.class).toString());
        o.setDoctorId(rs.getObject("doctor_id", UUID.class).toString());
        o.setTestName(rs.getString("test_name"));
        o.setStatus(rs.getString("status"));
        o.setOrderedAt(rs.getTimestamp("ordered_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        o.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        o.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return o;
    }
}
