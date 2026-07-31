package hospital.management.backend.service.department.interfaces;

import hospital.management.backend.dto.doctor.CreateDoctorScheduleDTO;
import hospital.management.backend.dto.doctor.DoctorScheduleDTO;

import java.util.List;

public interface DoctorScheduleService {
    DoctorScheduleDTO create(CreateDoctorScheduleDTO dto) throws Exception;
    List<DoctorScheduleDTO> findByDoctor(String doctorId) throws Exception;
    DoctorScheduleDTO update(String scheduleId, CreateDoctorScheduleDTO dto) throws Exception;
    void delete(String scheduleId) throws Exception;
}