package hospital.management.backend.mapper.pharmacy;

import hospital.management.backend.dto.pharmacy.CreatePrescriptionItemDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionItemDTO;
import hospital.management.backend.model.pharmacy.PrescriptionItem;

public class PrescriptionItemMapper {

    public static PrescriptionItemDTO toDTO(PrescriptionItem item) {
        if (item == null) return null;
        return new PrescriptionItemDTO(
            item.getItemId(),
            item.getPrescriptionId(),
            item.getMedicationId(),
            item.getDosage(),
            item.getQuantity(),
            item.getInstructions()
        );
    }

    public static PrescriptionItem toEntity(String prescriptionId, CreatePrescriptionItemDTO dto) {
        if (dto == null) return null;
        PrescriptionItem item = new PrescriptionItem();
        item.setPrescriptionId(prescriptionId);
        item.setMedicationId(dto.getMedicationId());
        item.setDosage(dto.getDosage());
        item.setQuantity(dto.getQuantity());
        item.setInstructions(dto.getInstructions());
        return item;
    }
}