package hospital.management.backend.dao.department;

import hospital.management.backend.dao.department.interfaces.DoctorScheduleDAO;
import hospital.management.backend.model.doctor.DoctorSchedule;

import java.util.List;
import java.util.Optional;

public class DoctorScheduleDAOImpl implements DoctorScheduleDAO {

    @Override
    public DoctorSchedule save(DoctorSchedule schedule) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<DoctorSchedule> findById(String scheduleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<DoctorSchedule> findByDoctorId(String doctorId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public DoctorSchedule update(DoctorSchedule schedule) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String scheduleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}