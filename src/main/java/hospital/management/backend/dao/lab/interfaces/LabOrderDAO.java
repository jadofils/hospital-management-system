package hospital.management.backend.dao.lab.interfaces;

import hospital.management.backend.model.lab.LabOrder;

import java.util.List;
import java.util.Optional;

public interface LabOrderDAO {
    LabOrder save(LabOrder order) throws Exception;
    Optional<LabOrder> findById(String labOrderId) throws Exception;
    List<LabOrder> findByAppointmentId(String appointmentId) throws Exception;
    List<LabOrder> findByDoctorId(String doctorId) throws Exception;
    LabOrder updateStatus(String labOrderId, String status) throws Exception;
    void softDelete(String labOrderId) throws Exception;
}