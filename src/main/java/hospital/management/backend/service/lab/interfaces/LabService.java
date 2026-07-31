package hospital.management.backend.service.lab.interfaces;

import hospital.management.backend.dto.lab.CreateLabOrderDTO;
import hospital.management.backend.dto.lab.CreateLabResultDTO;
import hospital.management.backend.dto.lab.LabOrderDTO;
import hospital.management.backend.dto.lab.LabResultDTO;

import java.util.List;

public interface LabService {
    LabOrderDTO orderTest(CreateLabOrderDTO dto) throws Exception;
    LabOrderDTO findOrderById(String labOrderId) throws Exception;
    List<LabOrderDTO> findOrdersByAppointment(String appointmentId) throws Exception;
    LabResultDTO recordResult(CreateLabResultDTO dto) throws Exception;
    LabResultDTO findResultByOrder(String labOrderId) throws Exception;
    void deleteOrder(String labOrderId) throws Exception;
}