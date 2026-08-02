package hospital.management.backend.dto.patient;

import java.math.BigDecimal;

public class CreateVitalSignDTO {

    private String     appointmentId;
    private Integer    bloodPressureSystolic;
    private Integer    bloodPressureDiastolic;
    private Integer    heartRate;
    private BigDecimal temperatureCelsius;
    private BigDecimal weightKg;
    private BigDecimal heightCm;

    public CreateVitalSignDTO() {}

    public CreateVitalSignDTO(String appointmentId, Integer bloodPressureSystolic,
                              Integer bloodPressureDiastolic, Integer heartRate,
                              BigDecimal temperatureCelsius, BigDecimal weightKg,
                              BigDecimal heightCm) {
        this.appointmentId          = appointmentId;
        this.bloodPressureSystolic  = bloodPressureSystolic;
        this.bloodPressureDiastolic = bloodPressureDiastolic;
        this.heartRate              = heartRate;
        this.temperatureCelsius     = temperatureCelsius;
        this.weightKg               = weightKg;
        this.heightCm               = heightCm;
    }

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

    @Override
    public String toString() {
        return "CreateVitalSignDTO{appointmentId='" + appointmentId + "'}";
    }
}