package hospital.management.backend.mapper.pharmacy;

import hospital.management.backend.dto.pharmacy.CreateMedicationDTO;
import hospital.management.backend.dto.pharmacy.MedicationDTO;
import hospital.management.backend.model.pharmacy.Medication;

public class MedicationMapper {

    public static MedicationDTO toDTO(Medication m) {
        if (m == null) return null;
        return new MedicationDTO(
            m.getMedicationId(),
            m.getName(),
            m.getGenericName(),
            m.getForm(),
            m.getUnitPrice(),
            m.getCreatedAt()
        );
    }

    public static Medication toEntity(CreateMedicationDTO dto) {
        if (dto == null) return null;
        Medication m = new Medication();
        m.setName(dto.getName());
        m.setGenericName(dto.getGenericName());
        m.setForm(dto.getForm());
        m.setUnitPrice(dto.getUnitPrice());
        return m;
    }
}