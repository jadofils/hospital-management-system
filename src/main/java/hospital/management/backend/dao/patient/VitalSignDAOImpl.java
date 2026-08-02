package hospital.management.backend.dao.patient;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.patient.interfaces.VitalSignDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.patient.VitalSign;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `vital_signs` table (see hospital_schema.sql). */
public class VitalSignDAOImpl implements VitalSignDAO {

    private static final String SELECT_COLUMNS =
        "vital_id, appointment_id, blood_pressure_systolic, blood_pressure_diastolic, "
      + "heart_rate, temperature_celsius, weight_kg, height_cm, recorded_at, updated_at, deleted_at";

    @Override
    public VitalSign save(VitalSign vitalSign) throws Exception {
        UUID id = vitalSign.getVitalId() != null ? UUID.fromString(vitalSign.getVitalId()) : UUID.randomUUID();
        String sql = "INSERT INTO vital_signs (vital_id, appointment_id, blood_pressure_systolic, "
                   + "blood_pressure_diastolic, heart_rate, temperature_celsius, weight_kg, height_cm) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING recorded_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(vitalSign.getAppointmentId()));
            setNullableInt(ps, 3, vitalSign.getBloodPressureSystolic());
            setNullableInt(ps, 4, vitalSign.getBloodPressureDiastolic());
            setNullableInt(ps, 5, vitalSign.getHeartRate());
            setNullableBigDecimal(ps, 6, vitalSign.getTemperatureCelsius());
            setNullableBigDecimal(ps, 7, vitalSign.getWeightKg());
            setNullableBigDecimal(ps, 8, vitalSign.getHeightCm());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    vitalSign.setVitalId(id.toString());
                    vitalSign.setRecordedAt(rs.getTimestamp("recorded_at").toLocalDateTime());
                    vitalSign.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save vital sign: " + e.getMessage(), e);
        }
        return vitalSign;
    }

    @Override
    public Optional<VitalSign> findById(String vitalId) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM vital_signs WHERE vital_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(vitalId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up vital sign: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<VitalSign> findByAppointmentId(String appointmentId) throws Exception {
        // An appointment can in principle have more than one recorded reading over time;
        // the DAO contract returns a single Optional, so the most recent one wins.
        String sql = "SELECT " + SELECT_COLUMNS + " FROM vital_signs "
                   + "WHERE appointment_id = ? AND deleted_at IS NULL ORDER BY recorded_at DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(appointmentId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up vital sign by appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public List<VitalSign> findByPatientId(String patientId) throws Exception {
        // vital_signs has no patient_id column — it is scoped to an appointment, which is
        // scoped to a patient. Join through appointments to answer "all vitals for patient X".
        String sql = "SELECT v.vital_id, v.appointment_id, v.blood_pressure_systolic, v.blood_pressure_diastolic, "
                   + "v.heart_rate, v.temperature_celsius, v.weight_kg, v.height_cm, v.recorded_at, v.updated_at, v.deleted_at "
                   + "FROM vital_signs v "
                   + "JOIN appointments a ON a.appointment_id = v.appointment_id "
                   + "WHERE a.patient_id = ? AND v.deleted_at IS NULL "
                   + "ORDER BY v.recorded_at DESC";
        List<VitalSign> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(patientId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list vital signs for patient: " + e.getMessage(), e);
        }
        return rows;
    }

    @Override
    public void softDelete(String vitalId) throws Exception {
        String sql = "UPDATE vital_signs SET deleted_at = CURRENT_TIMESTAMP WHERE vital_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(vitalId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("VitalSign", vitalId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete vital sign: " + e.getMessage(), e);
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) ps.setInt(index, value);
        else ps.setNull(index, Types.SMALLINT);
    }

    private void setNullableBigDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value != null) ps.setBigDecimal(index, value);
        else ps.setNull(index, Types.DECIMAL);
    }

    private VitalSign mapRow(ResultSet rs) throws SQLException {
        VitalSign v = new VitalSign();
        v.setVitalId(rs.getObject("vital_id", UUID.class).toString());
        v.setAppointmentId(rs.getObject("appointment_id", UUID.class).toString());
        v.setBloodPressureSystolic(getNullableInt(rs, "blood_pressure_systolic"));
        v.setBloodPressureDiastolic(getNullableInt(rs, "blood_pressure_diastolic"));
        v.setHeartRate(getNullableInt(rs, "heart_rate"));
        v.setTemperatureCelsius(rs.getBigDecimal("temperature_celsius"));
        v.setWeightKg(rs.getBigDecimal("weight_kg"));
        v.setHeightCm(rs.getBigDecimal("height_cm"));
        v.setRecordedAt(rs.getTimestamp("recorded_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        v.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        v.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return v;
    }

    private Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
