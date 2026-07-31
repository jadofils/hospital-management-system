package hospital.management.backend.service.department;

import hospital.management.backend.dao.department.interfaces.DoctorScheduleDAO;
import hospital.management.backend.dto.doctor.CreateDoctorScheduleDTO;
import hospital.management.backend.dto.doctor.DoctorScheduleDTO;
import hospital.management.backend.service.department.interfaces.DoctorScheduleService;

import java.util.List;

public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    private final DoctorScheduleDAO scheduleDAO;

    public DoctorScheduleServiceImpl(DoctorScheduleDAO scheduleDAO) {
        this.scheduleDAO = scheduleDAO;
    }

    @Override
    public DoctorScheduleDTO create(CreateDoctorScheduleDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<DoctorScheduleDTO> findByDoctor(String doctorId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public DoctorScheduleDTO update(String scheduleId, CreateDoctorScheduleDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String scheduleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}