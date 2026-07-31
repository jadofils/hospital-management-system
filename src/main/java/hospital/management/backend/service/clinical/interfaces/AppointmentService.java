package hospital.management.backend.service.clinical.interfaces;

import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.clinical.CreateAppointmentDTO;
import hospital.management.backend.dto.clinical.UpdateAppointmentDTO;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;

public interface AppointmentService {
    AppointmentDTO book(CreateAppointmentDTO dto) throws Exception;
    AppointmentDTO findById(String appointmentId) throws Exception;
    PageResult<AppointmentSummaryDTO> findAll(PageRequest request) throws Exception;
    List<AppointmentDTO> findByPatient(String patientId) throws Exception;
    List<AppointmentDTO> findByDoctor(String doctorId) throws Exception;
    AppointmentDTO update(UpdateAppointmentDTO dto) throws Exception;
    void cancel(String appointmentId) throws Exception;
}