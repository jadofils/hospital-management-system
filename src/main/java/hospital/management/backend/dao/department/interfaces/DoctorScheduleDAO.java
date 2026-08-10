package hospital.management.backend.dao.department.interfaces;

import hospital.management.backend.model.doctor.DoctorSchedule;

import java.util.List;
import java.util.Optional;

public interface DoctorScheduleDAO {
    DoctorSchedule save(DoctorSchedule schedule) throws Exception;
    Optional<DoctorSchedule> findById(String scheduleId) throws Exception;
    List<DoctorSchedule> findByDoctorId(String doctorId) throws Exception;
    /** All non-deleted schedule rows across every doctor, used to filter doctors by day availability. */
    List<DoctorSchedule> findAll() throws Exception;
    DoctorSchedule update(DoctorSchedule schedule) throws Exception;
    void softDelete(String scheduleId) throws Exception;
}