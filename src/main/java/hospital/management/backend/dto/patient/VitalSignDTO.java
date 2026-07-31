package hospital.management.backend.dto.patient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VitalSignDTO {

    private String        vitalId;
    private String        appointmentId;
    private Integer       bloodPressureSystolic;
    private Integer       bloodPressureDiastolic;
    private Integer       heartRate;
    private BigDecimal    temperatureCelsius;
    private BigDecimal    weightKg;
    private BigDecimal    heightCm;
    private LocalDateTime recordedAt;

    public VitalSignDTO() {}

    public VitalSignDTO(String vitalId, String appointmentId,
                        Integer bloodPressureSystolic, Integer bloodPressureDiastolic,
                        Integer heartRate, BigDecimal temperatureCelsius,
                        BigDecimal weightKg, BigDecimal heightCm, LocalDateTime recordedAt) {
        this.vitalId                = vitalId;
        this.appointmentId          = appointmentId;
        this.bloodPressureSystolic  = bloodPressureSystolic;
        this.bloodPressureDiastolic = bloodPressureDiastolic;
        this.heartRate              = heartRate;
        this.temperatureCelsius     = temperatureCelsius;
        this.weightKg               = weightKg;
        this.heightCm               = heightCm;
        this.recordedAt             = recordedAt;
    }

    public String getVitalId() { return vitalId; }
    public void setVitalId(String vitalId) { this.vitalId = vitalId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public Integer getBloodPressureSystolic() { return bloodPressureSystolic; }
    public void setBloodPressureSystolic(Integer v) { this.bloodPressureSystolic = v; }

    public Integer getBloodPressureDiastolic() { return bloodPressureDiastolic; }
    public void setBloodPressureDiastolic(Integer v) { this.bloodPressureDiastolic = v; }

    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }

    public BigDecimal getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(BigDecimal v) { this.temperatureCelsius = v; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    @Override
    public String toString() {
        return "VitalSignDTO{vitalId='" + vitalId + "', appointmentId='" + appointmentId + "'}";
    }
}