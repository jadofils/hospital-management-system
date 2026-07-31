package hospital.management.backend.service.lab;

import hospital.management.backend.dao.lab.interfaces.LabOrderDAO;
import hospital.management.backend.dao.lab.interfaces.LabResultDAO;
import hospital.management.backend.dto.lab.CreateLabOrderDTO;
import hospital.management.backend.dto.lab.CreateLabResultDTO;
import hospital.management.backend.dto.lab.LabOrderDTO;
import hospital.management.backend.dto.lab.LabResultDTO;
import hospital.management.backend.service.lab.interfaces.LabService;

import java.util.List;

public class LabServiceImpl implements LabService {

    private final LabOrderDAO  labOrderDAO;
    private final LabResultDAO labResultDAO;

    public LabServiceImpl(LabOrderDAO labOrderDAO, LabResultDAO labResultDAO) {
        this.labOrderDAO  = labOrderDAO;
        this.labResultDAO = labResultDAO;
    }

    @Override
    public LabOrderDTO orderTest(CreateLabOrderDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public LabOrderDTO findOrderById(String labOrderId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<LabOrderDTO> findOrdersByAppointment(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public LabResultDTO recordResult(CreateLabResultDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public LabResultDTO findResultByOrder(String labOrderId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deleteOrder(String labOrderId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}