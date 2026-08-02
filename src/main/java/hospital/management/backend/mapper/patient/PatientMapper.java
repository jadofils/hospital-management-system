package hospital.management.backend.mapper.patient;

import hospital.management.backend.dto.patient.CreatePatientDTO;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.patient.PatientSummaryDTO;
import hospital.management.backend.model.patient.Patient;

public class PatientMapper {

    public static PatientDTO toDTO(Patient patient) {
        if (patient == null) return null;
        return new PatientDTO(
            patient.getPatientId(),
            patient.getFirstName(),
            patient.getLastName(),
            patient.getDob(),
            patient.getGender(),
            patient.getPhone(),
            patient.getEmail(),
            patient.getAddress(),
            patient.getCreatedAt(),
            patient.getUpdatedAt()
        );
    }

    public static PatientSummaryDTO toSummaryDTO(Patient patient) {
        if (patient == null) return null;
        return new PatientSummaryDTO(
            patient.getPatientId(),
            patient.getFullName(),
            patient.getGender(),
            patient.getPhone(),
            patient.getEmail()
        );
    }

    public static Patient toEntity(CreatePatientDTO dto) {
        if (dto == null) return null;
        Patient p = new Patient();
        p.setFirstName(dto.getFirstName());
        p.setLastName(dto.getLastName());
        p.setDob(dto.getDob());
        p.setGender(dto.getGender());
        p.setPhone(dto.getPhone());
        p.setEmail(dto.getEmail());
        p.setAddress(dto.getAddress());
        return p;
    }
}