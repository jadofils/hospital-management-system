package hospital.management.backend.dto.doctor;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class DoctorScheduleDTO {

    private String        scheduleId;
    private String        doctorId;
    private String        dayOfWeek;
    private LocalTime     startTime;
    private LocalTime     endTime;
    private Boolean       isAvailable;
    private LocalDateTime createdAt;

    public DoctorScheduleDTO() {}

    public DoctorScheduleDTO(String scheduleId, String doctorId, String dayOfWeek,
                             LocalTime startTime, LocalTime endTime, Boolean isAvailable,
                             LocalDateTime createdAt) {
        this.scheduleId  = scheduleId;
        this.doctorId    = doctorId;
        this.dayOfWeek   = dayOfWeek;
        this.startTime   = startTime;
        this.endTime     = endTime;
        this.isAvailable = isAvailable;
        this.createdAt   = createdAt;
    }

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "DoctorScheduleDTO{scheduleId='" + scheduleId + "', doctorId='" + doctorId + "', day='" + dayOfWeek + "'}";
    }
}