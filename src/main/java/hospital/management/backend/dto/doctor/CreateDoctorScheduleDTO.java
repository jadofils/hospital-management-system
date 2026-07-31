package hospital.management.backend.dto.doctor;

import java.time.LocalTime;

public class CreateDoctorScheduleDTO {

    private String    doctorId;
    private String    dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean   isAvailable;

    public CreateDoctorScheduleDTO() {}

    public CreateDoctorScheduleDTO(String doctorId, String dayOfWeek,
                                   LocalTime startTime, LocalTime endTime, Boolean isAvailable) {
        this.doctorId    = doctorId;
        this.dayOfWeek   = dayOfWeek;
        this.startTime   = startTime;
        this.endTime     = endTime;
        this.isAvailable = isAvailable;
    }

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

    @Override
    public String toString() {
        return "CreateDoctorScheduleDTO{doctorId='" + doctorId + "', day='" + dayOfWeek + "'}";
    }
}