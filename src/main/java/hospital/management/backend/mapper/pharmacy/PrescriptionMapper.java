package hospital.management.backend.mapper.pharmacy;

import hospital.management.backend.dto.pharmacy.CreatePrescriptionDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import hospital.management.backend.model.pharmacy.Prescription;

public class PrescriptionMapper {

    public static PrescriptionDTO toDTO(Prescription p) {
        if (p == null) return null;
        return new PrescriptionDTO(
            p.getPrescriptionId(),
            p.getAppointmentId(),
            p.getDateIssued(),
            p.getCreatedAt(),
            null  // items loaded separately by service
        );
    }

    public static Prescription toEntity(CreatePrescriptionDTO dto) {
        if (dto == null) return null;
        Prescription p = new Prescription();
        p.setAppointmentId(dto.getAppointmentId());
        p.setDateIssued(dto.getDateIssued());
        return p;
    }
}