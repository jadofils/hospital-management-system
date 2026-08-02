package hospital.management.backend.mapper.lab;

import hospital.management.backend.dto.lab.CreateLabResultDTO;
import hospital.management.backend.dto.lab.LabResultDTO;
import hospital.management.backend.model.lab.LabResult;

public class LabResultMapper {

    public static LabResultDTO toDTO(LabResult r) {
        if (r == null) return null;
        return new LabResultDTO(
            r.getLabResultId(),
            r.getLabOrderId(),
            r.getResultValue(),
            r.getUnit(),
            r.getReferenceRange(),
            r.isIsAbnormal(),
            r.getCompletedAt()
        );
    }

    public static LabResult toEntity(CreateLabResultDTO dto) {
        if (dto == null) return null;
        LabResult r = new LabResult();
        r.setLabOrderId(dto.getLabOrderId());
        r.setResultValue(dto.getResultValue());
        r.setUnit(dto.getUnit());
        r.setReferenceRange(dto.getReferenceRange());
        r.setIsAbnormal(dto.getIsAbnormal());
        r.setCompletedAt(dto.getCompletedAt());
        return r;
    }
}