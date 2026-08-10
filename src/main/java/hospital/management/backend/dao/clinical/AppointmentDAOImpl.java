package hospital.management.backend.dao.clinical;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.clinical.interfaces.AppointmentDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.utils.filters.QueryBuilder;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `appointments` table (see hospital_schema.sql). */
public class AppointmentDAOImpl implements AppointmentDAO {

    private static final String SELECT_COLUMNS =
        "appointment_id, patient_id, doctor_id, appointment_date, status, reason, created_at, updated_at, deleted_at";

    @Override
    public Appointment save(Appointment appointment) throws Exception {
        UUID id = appointment.getAppointmentId() != null
                ? UUID.fromString(appointment.getAppointmentId()) : UUID.randomUUID();
        String sql = "INSERT INTO appointments (appointment_id, patient_id, doctor_id, appointment_date, status, reason) "
                   + "VALUES (?, ?, ?, ?, ?, ?) RETURNING created_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(appointment.getPatientId()));
            ps.setObject(3, UUID.fromString(appointment.getDoctorId()));
            ps.setTimestamp(4, Timestamp.valueOf(appointment.getAppointmentDate()));
            ps.setString(5, appointment.getStatus() != null ? appointment.getStatus() : "scheduled");
            ps.setString(6, appointment.getReason());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    appointment.setAppointmentId(id.toString());
                    if (appointment.getStatus() == null) appointment.setStatus("scheduled");
                    appointment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    appointment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save appointment: " + e.getMessage(), e);
        }
        return appointment;
    }

    @Override
    public Optional<Appointment> findById(String appointmentId) throws Exception {
        return findOneWhere("appointment_id = ?", UUID.fromString(appointmentId));
    }

    private Optional<Appointment> findOneWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM appointments WHERE " + predicate + " AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public PageResult<Appointment> findAll(PageRequest request) throws Exception {
        QueryBuilder.SortDir dir = request.getDirection() == PageRequest.SortDirection.DESC
            ? QueryBuilder.SortDir.DESC : QueryBuilder.SortDir.ASC;
        QueryBuilder qb = QueryBuilder.select(SELECT_COLUMNS).from("appointments").whereActive();
        String cursorFrag = CursorPagination.whereClause(request, "created_at");
        if (!cursorFrag.isBlank()) qb.and(cursorFrag.trim().replaceFirst("(?i)^AND\\s+", ""));
        String sql = qb.orderBy("created_at", dir).limit(request.getPageSize() + 1).build();
        List<Appointment> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list appointments: " + e.getMessage(), e);
        }
        return CursorPagination.toResult(rows, request, Appointment::getCreatedAt);
    }

    @Override
    public List<Appointment> findByPatientId(String patientId) throws Exception {
        return findAllWhere("patient_id = ?", UUID.fromString(patientId));
    }

    @Override
    public List<Appointment> findByDoctorId(String doctorId) throws Exception {
        return findAllWhere("doctor_id = ?", UUID.fromString(doctorId));
    }

    private List<Appointment> findAllWhere(String predicate, Object param) throws Exception {
        String sql = QueryBuilder.select(SELECT_COLUMNS)
            .from("appointments")
            .where(predicate)
            .whereActive()
            .orderBy("appointment_date", QueryBuilder.SortDir.DESC)
            .build();
        List<Appointment> appointments = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) appointments.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list appointments: " + e.getMessage(), e);
        }
        return appointments;
    }

    @Override
    public Appointment update(Appointment appointment) throws Exception {
        String sql = "UPDATE appointments SET appointment_date = ?, status = ?, reason = ? "
                   + "WHERE appointment_id = ? AND deleted_at IS NULL RETURNING updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(appointment.getAppointmentDate()));
            ps.setString(2, appointment.getStatus());
            ps.setString(3, appointment.getReason());
            ps.setObject(4, UUID.fromString(appointment.getAppointmentId()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ResourceNotFoundException("Appointment", appointment.getAppointmentId());
                appointment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update appointment: " + e.getMessage(), e);
        }
        return appointment;
    }

    @Override
    public void softDelete(String appointmentId) throws Exception {
        String sql = "UPDATE appointments SET deleted_at = CURRENT_TIMESTAMP WHERE appointment_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(appointmentId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("Appointment", appointmentId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete appointment: " + e.getMessage(), e);
        }
    }

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentId(rs.getObject("appointment_id", UUID.class).toString());
        a.setPatientId(rs.getObject("patient_id", UUID.class).toString());
        a.setDoctorId(rs.getObject("doctor_id", UUID.class).toString());
        a.setAppointmentDate(rs.getTimestamp("appointment_date").toLocalDateTime());
        a.setStatus(rs.getString("status"));
        a.setReason(rs.getString("reason"));
        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        a.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        a.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return a;
    }
}
