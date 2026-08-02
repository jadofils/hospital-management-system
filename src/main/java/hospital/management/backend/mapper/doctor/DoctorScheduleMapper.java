package hospital.management.backend.mapper.doctor;

import hospital.management.backend.dto.doctor.CreateDoctorScheduleDTO;
import hospital.management.backend.dto.doctor.DoctorScheduleDTO;
import hospital.management.backend.model.doctor.DoctorSchedule;

public class DoctorScheduleMapper {

    public static DoctorScheduleDTO toDTO(DoctorSchedule s) {
        if (s == null) return null;
        return new DoctorScheduleDTO(
            s.getScheduleId(),
            s.getDoctorId(),
            s.getDayOfWeek(),
            s.getStartTime(),
            s.getEndTime(),
            s.isIsAvailable(),
            s.getCreatedAt()
        );
    }

    public static DoctorSchedule toEntity(CreateDoctorScheduleDTO dto) {
        if (dto == null) return null;
        DoctorSchedule s = new DoctorSchedule();
        s.setDoctorId(dto.getDoctorId());
        s.setDayOfWeek(dto.getDayOfWeek());
        s.setStartTime(dto.getStartTime());
        s.setEndTime(dto.getEndTime());
        s.setIsAvailable(dto.getIsAvailable());
        return s;
    }
}