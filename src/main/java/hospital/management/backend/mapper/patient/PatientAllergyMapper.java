package hospital.management.backend.mapper.patient;

import hospital.management.backend.dto.patient.CreatePatientAllergyDTO;
import hospital.management.backend.dto.patient.PatientAllergyDTO;
import hospital.management.backend.model.patient.PatientAllergy;

public class PatientAllergyMapper {

    public static PatientAllergyDTO toDTO(PatientAllergy a) {
        if (a == null) return null;
        return new PatientAllergyDTO(
            a.getAllergyId(),
            a.getPatientId(),
            a.getAllergen(),
            a.getReaction(),
            a.getSeverity(),
            a.getCreatedAt()
        );
    }

    public static PatientAllergy toEntity(CreatePatientAllergyDTO dto) {
        if (dto == null) return null;
        PatientAllergy a = new PatientAllergy();
        a.setPatientId(dto.getPatientId());
        a.setAllergen(dto.getAllergen());
        a.setReaction(dto.getReaction());
        a.setSeverity(dto.getSeverity());
        return a;
    }
}