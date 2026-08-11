package hospital.management.backend.mapper.clinical;

import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.clinical.CreateAppointmentDTO;
import hospital.management.backend.model.patient.Appointment;

public class AppointmentMapper {

    public static AppointmentDTO toDTO(Appointment a) {
        if (a == null) return null;
        return new AppointmentDTO(
            a.getAppointmentId(),
            a.getPatientId(),
            a.getDoctorId(),
            a.getAppointmentDate(),
            a.getStatus(),
            a.getReason(),
            a.getCreatedAt(),
            a.getUpdatedAt()
        );
    }

    public static AppointmentSummaryDTO toSummaryDTO(Appointment a,
                                                     String patientName,
                                                     String doctorName) {
        if (a == null) return null;
        return new AppointmentSummaryDTO(
            a.getAppointmentId(),
            a.getPatientId(),
            patientName,
            a.getDoctorId(),
            doctorName,
            a.getAppointmentDate(),
            a.getStatus()
        );
    }

    public static Appointment toEntity(CreateAppointmentDTO dto) {
        if (dto == null) return null;
        Appointment a = new Appointment();
        a.setPatientId(dto.getPatientId());
        a.setDoctorId(dto.getDoctorId());
        a.setAppointmentDate(dto.getAppointmentDate());
        a.setReason(dto.getReason());
        a.setStatus("scheduled");
        return a;
    }
}