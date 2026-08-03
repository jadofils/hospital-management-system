package hospital.management.backend.mapper.patient;

import hospital.management.backend.dto.patient.CreatePatientDTO;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.patient.PatientSummaryDTO;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.enums.Gender;
import hospital.management.backend.model.patient.Patient;

public class PatientMapper {

    public static PatientDTO toDTO(Patient patient) {
        if (patient == null) return null;
        String label = null;
        try {
            label = patient.getGender() == null ? null : Gender.fromDbValue(patient.getGender()).getLabel();
        } catch (Exception ignored) {
            label = patient.getGender();
        }
        return new PatientDTO(
            patient.getPatientId(),
            patient.getFirstName(),
            patient.getLastName(),
            patient.getDob(),
            label,
            patient.getPhone(),
            patient.getEmail(),
            patient.getAddress(),
            patient.getCreatedAt(),
            patient.getUpdatedAt()
        );
    }

    public static PatientSummaryDTO toSummaryDTO(Patient patient) {
        if (patient == null) return null;
        String label = null;
        try {
            label = patient.getGender() == null ? null : Gender.fromDbValue(patient.getGender()).getLabel();
        } catch (Exception ignored) {
            label = patient.getGender();
        }
        return new PatientSummaryDTO(
            patient.getPatientId(),
            patient.getFullName(),
            label,
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
        // Map UI label (e.g. "Male") to DB value ("M") using the enum; throw ValidationException if unknown.
        if (dto.getGender() != null && !dto.getGender().isBlank()) {
            try {
                Gender g = Gender.fromLabel(dto.getGender());
                p.setGender(g.getDbValue());
            } catch (IllegalArgumentException ex) {
                throw new ValidationException("gender", "Unknown gender: " + dto.getGender());
            }
        }
        p.setPhone(dto.getPhone());
        p.setEmail(dto.getEmail());
        p.setAddress(dto.getAddress());
        return p;
    }

}