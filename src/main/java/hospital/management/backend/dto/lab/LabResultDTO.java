package hospital.management.backend.dto.lab;

import java.time.LocalDateTime;

public class LabResultDTO {

    private String        labResultId;
    private String        labOrderId;
    private String        resultValue;
    private String        unit;
    private String        referenceRange;
    private Boolean       isAbnormal;
    private LocalDateTime completedAt;

    public LabResultDTO() {}

    public LabResultDTO(String labResultId, String labOrderId, String resultValue,
                        String unit, String referenceRange, Boolean isAbnormal,
                        LocalDateTime completedAt) {
        this.labResultId    = labResultId;
        this.labOrderId     = labOrderId;
        this.resultValue    = resultValue;
        this.unit           = unit;
        this.referenceRange = referenceRange;
        this.isAbnormal     = isAbnormal;
        this.completedAt    = completedAt;
    }

    public String getLabResultId() { return labResultId; }
    public void setLabResultId(String labResultId) { this.labResultId = labResultId; }

    public String getLabOrderId() { return labOrderId; }
    public void setLabOrderId(String labOrderId) { this.labOrderId = labOrderId; }

    public String getResultValue() { return resultValue; }
    public void setResultValue(String resultValue) { this.resultValue = resultValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getReferenceRange() { return referenceRange; }
    public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }

    public Boolean getIsAbnormal() { return isAbnormal; }
    public void setIsAbnormal(Boolean isAbnormal) { this.isAbnormal = isAbnormal; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    @Override
    public String toString() {
        return "LabResultDTO{labResultId='" + labResultId + "', labOrderId='" + labOrderId + "'}";
    }
}