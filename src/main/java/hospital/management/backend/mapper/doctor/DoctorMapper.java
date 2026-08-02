package hospital.management.backend.mapper.doctor;

import hospital.management.backend.dto.doctor.CreateDoctorDTO;
import hospital.management.backend.dto.doctor.DoctorDTO;
import hospital.management.backend.dto.doctor.DoctorSummaryDTO;
import hospital.management.backend.model.doctor.Doctor;

public class DoctorMapper {

    public static DoctorDTO toDTO(Doctor doctor) {
        if (doctor == null) return null;
        return new DoctorDTO(
            doctor.getDoctorId(),
            doctor.getDepartmentId(),
            doctor.getFirstName(),
            doctor.getLastName(),
            doctor.getSpecialization(),
            doctor.getPhone(),
            doctor.getEmail(),
            doctor.getCreatedAt(),
            doctor.getUpdatedAt()
        );
    }

    public static DoctorSummaryDTO toSummaryDTO(Doctor doctor, String departmentName) {
        if (doctor == null) return null;
        return new DoctorSummaryDTO(
            doctor.getDoctorId(),
            doctor.getFullName(),
            doctor.getSpecialization(),
            departmentName
        );
    }

    public static Doctor toEntity(CreateDoctorDTO dto) {
        if (dto == null) return null;
        Doctor d = new Doctor();
        d.setDepartmentId(dto.getDepartmentId());
        d.setFirstName(dto.getFirstName());
        d.setLastName(dto.getLastName());
        d.setSpecialization(dto.getSpecialization());
        d.setPhone(dto.getPhone());
        d.setEmail(dto.getEmail());
        return d;
    }
}