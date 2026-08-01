package hospital.management.backend.dao.lab.interfaces;

import hospital.management.backend.model.lab.LabOrder;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface LabOrderDAO {
    LabOrder save(LabOrder order) throws Exception;

    /** Transaction-composable overload — participates in a caller-owned connection/commit. */
    LabOrder save(LabOrder order, Connection conn) throws Exception;

    Optional<LabOrder> findById(String labOrderId) throws Exception;
    List<LabOrder> findByAppointmentId(String appointmentId) throws Exception;
    List<LabOrder> findByDoctorId(String doctorId) throws Exception;

    LabOrder updateStatus(String labOrderId, String status) throws Exception;

    /** Transaction-composable overload — participates in a caller-owned connection/commit. */
    LabOrder updateStatus(String labOrderId, String status, Connection conn) throws Exception;

    void softDelete(String labOrderId) throws Exception;
}