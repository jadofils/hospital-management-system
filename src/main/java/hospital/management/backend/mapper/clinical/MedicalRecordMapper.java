package hospital.management.backend.mapper.clinical;

import hospital.management.backend.dto.clinical.CreateMedicalRecordDTO;
import hospital.management.backend.dto.clinical.MedicalRecordDTO;
import hospital.management.backend.model.patient.MedicalRecord;

public class MedicalRecordMapper {

    public static MedicalRecordDTO toDTO(MedicalRecord r) {
        if (r == null) return null;
        return new MedicalRecordDTO(
            r.getRecordId(),
            r.getAppointmentId(),
            r.getDiagnosis(),
            r.getSymptoms(),
            r.getNotes(),
            r.getCreatedAt()
        );
    }

    public static MedicalRecord toEntity(CreateMedicalRecordDTO dto) {
        if (dto == null) return null;
        MedicalRecord r = new MedicalRecord();
        r.setAppointmentId(dto.getAppointmentId());
        r.setDiagnosis(dto.getDiagnosis());
        r.setSymptoms(dto.getSymptoms());
        r.setNotes(dto.getNotes());
        return r;
    }
}