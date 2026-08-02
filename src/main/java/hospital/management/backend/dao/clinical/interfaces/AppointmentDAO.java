package hospital.management.backend.dao.clinical.interfaces;

import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;
import java.util.Optional;

public interface AppointmentDAO {
    Appointment save(Appointment appointment) throws Exception;
    Optional<Appointment> findById(String appointmentId) throws Exception;
    PageResult<Appointment> findAll(PageRequest request) throws Exception;
    List<Appointment> findByPatientId(String patientId) throws Exception;
    List<Appointment> findByDoctorId(String doctorId) throws Exception;
    Appointment update(Appointment appointment) throws Exception;
    void softDelete(String appointmentId) throws Exception;
}