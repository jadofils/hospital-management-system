package hospital.management.backend.mapper.patient;

import hospital.management.backend.dto.patient.CreateVitalSignDTO;
import hospital.management.backend.dto.patient.VitalSignDTO;
import hospital.management.backend.model.patient.VitalSign;

public class VitalSignMapper {

    public static VitalSignDTO toDTO(VitalSign v) {
        if (v == null) return null;
        return new VitalSignDTO(
            v.getVitalId(),
            v.getAppointmentId(),
            v.getBloodPressureSystolic(),
            v.getBloodPressureDiastolic(),
            v.getHeartRate(),
            v.getTemperatureCelsius(),
            v.getWeightKg(),
            v.getHeightCm(),
            v.getRecordedAt()
        );
    }

    public static VitalSign toEntity(CreateVitalSignDTO dto) {
        if (dto == null) return null;
        VitalSign v = new VitalSign();
        v.setAppointmentId(dto.getAppointmentId());
        v.setBloodPressureSystolic(dto.getBloodPressureSystolic());
        v.setBloodPressureDiastolic(dto.getBloodPressureDiastolic());
        v.setHeartRate(dto.getHeartRate());
        v.setTemperatureCelsius(dto.getTemperatureCelsius());
        v.setWeightKg(dto.getWeightKg());
        v.setHeightCm(dto.getHeightCm());
        return v;
    }
}