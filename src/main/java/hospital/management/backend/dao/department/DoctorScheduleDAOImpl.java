package hospital.management.backend.dao.department;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.department.interfaces.DoctorScheduleDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.DoctorSchedule;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `doctor_schedules` table (see hospital_schema.sql). */
public class DoctorScheduleDAOImpl implements DoctorScheduleDAO {

    private static final String SELECT_COLUMNS =
        "schedule_id, doctor_id, day_of_week, start_time, end_time, is_available, created_at, updated_at, deleted_at";

    /** Orders Mon..Sun in calendar order rather than alphabetically. */
    private static final String DAY_ORDER_CLAUSE =
        "CASE day_of_week " +
        "WHEN 'Mon' THEN 1 WHEN 'Tue' THEN 2 WHEN 'Wed' THEN 3 WHEN 'Thu' THEN 4 " +
        "WHEN 'Fri' THEN 5 WHEN 'Sat' THEN 6 WHEN 'Sun' THEN 7 END, start_time";

    @Override
    public DoctorSchedule save(DoctorSchedule schedule) throws Exception {
        UUID id = schedule.getScheduleId() != null ? UUID.fromString(schedule.getScheduleId()) : UUID.randomUUID();
        String sql = "INSERT INTO doctor_schedules (schedule_id, doctor_id, day_of_week, start_time, end_time, is_available) "
                   + "VALUES (?, ?, ?, ?, ?, ?) RETURNING created_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(schedule.getDoctorId()));
            ps.setString(3, schedule.getDayOfWeek());
            ps.setObject(4, schedule.getStartTime());
            ps.setObject(5, schedule.getEndTime());
            ps.setBoolean(6, schedule.isIsAvailable() != null ? schedule.isIsAvailable() : true);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    schedule.setScheduleId(id.toString());
                    schedule.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    schedule.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save doctor schedule: " + e.getMessage(), e);
        }
        return schedule;
    }

    @Override
    public Optional<DoctorSchedule> findById(String scheduleId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM doctor_schedules WHERE schedule_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(scheduleId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up doctor schedule: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DoctorSchedule> findByDoctorId(String doctorId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM doctor_schedules WHERE doctor_id = ? AND deleted_at IS NULL "
                   + "ORDER BY " + DAY_ORDER_CLAUSE;
        List<DoctorSchedule> schedules = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(doctorId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) schedules.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list doctor schedules: " + e.getMessage(), e);
        }
        return schedules;
    }

    @Override
    public DoctorSchedule update(DoctorSchedule schedule) throws Exception {
        String sql = "UPDATE doctor_schedules SET day_of_week = ?, start_time = ?, end_time = ?, is_available = ? "
                   + "WHERE schedule_id = ? AND deleted_at IS NULL RETURNING updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schedule.getDayOfWeek());
            ps.setObject(2, schedule.getStartTime());
            ps.setObject(3, schedule.getEndTime());
            ps.setBoolean(4, schedule.isIsAvailable() != null ? schedule.isIsAvailable() : true);
            ps.setObject(5, UUID.fromString(schedule.getScheduleId()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ResourceNotFoundException("DoctorSchedule", schedule.getScheduleId());
                schedule.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update doctor schedule: " + e.getMessage(), e);
        }
        return schedule;
    }

    @Override
    public void softDelete(String scheduleId) throws Exception {
        String sql = "UPDATE doctor_schedules SET deleted_at = CURRENT_TIMESTAMP WHERE schedule_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(scheduleId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("DoctorSchedule", scheduleId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete doctor schedule: " + e.getMessage(), e);
        }
    }

    private DoctorSchedule mapRow(ResultSet rs) throws SQLException {
        DoctorSchedule s = new DoctorSchedule();
        s.setScheduleId(rs.getObject("schedule_id", UUID.class).toString());
        s.setDoctorId(rs.getObject("doctor_id", UUID.class).toString());
        s.setDayOfWeek(rs.getString("day_of_week"));
        s.setStartTime(rs.getObject("start_time", LocalTime.class));
        s.setEndTime(rs.getObject("end_time", LocalTime.class));
        s.setIsAvailable(rs.getBoolean("is_available"));
        s.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        s.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        s.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return s;
    }
}
