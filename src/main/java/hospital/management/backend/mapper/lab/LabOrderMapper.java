package hospital.management.backend.mapper.lab;

import hospital.management.backend.dto.lab.CreateLabOrderDTO;
import hospital.management.backend.dto.lab.LabOrderDTO;
import hospital.management.backend.model.enums.LabOrderStatus;
import hospital.management.backend.model.lab.LabOrder;

public class LabOrderMapper {

    public static LabOrderDTO toDTO(LabOrder o) {
        if (o == null) return null;
        return new LabOrderDTO(
            o.getLabOrderId(),
            o.getAppointmentId(),
            o.getDoctorId(),
            o.getTestName(),
            o.getStatus(),
            o.getOrderedAt()
        );
    }

    public static LabOrder toEntity(CreateLabOrderDTO dto) {
        if (dto == null) return null;
        LabOrder o = new LabOrder();
        o.setAppointmentId(dto.getAppointmentId());
        o.setDoctorId(dto.getDoctorId());
        o.setTestName(dto.getTestName());
        // DB CHECK constraint on lab_orders.status only allows ordered/in_progress/completed/cancelled.
        o.setStatus(LabOrderStatus.ORDERED.getDbValue());
        return o;
    }
}