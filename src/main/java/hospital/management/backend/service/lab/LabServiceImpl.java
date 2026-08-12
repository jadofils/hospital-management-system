package hospital.management.backend.service.lab;

import com.fasterxml.jackson.core.type.TypeReference;
import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.config.db.TransactionManager;
import hospital.management.backend.dao.lab.interfaces.LabOrderDAO;
import hospital.management.backend.dao.lab.interfaces.LabResultDAO;
import hospital.management.backend.dto.lab.CreateLabOrderDTO;
import hospital.management.backend.dto.lab.CreateLabResultDTO;
import hospital.management.backend.dto.lab.LabOrderDTO;
import hospital.management.backend.dto.lab.LabResultDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.lab.LabOrderMapper;
import hospital.management.backend.mapper.lab.LabResultMapper;
import hospital.management.backend.model.enums.LabOrderStatus;
import hospital.management.backend.model.lab.LabOrder;
import hospital.management.backend.model.lab.LabResult;
import hospital.management.backend.service.lab.interfaces.LabService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Owns lab order + lab result workflows. Recording a result is treated as the
 * event that finalizes its parent order: the result INSERT and the order's
 * status flip to "completed" happen inside one {@link TransactionManager}
 * unit of work so a result is never persisted against an order that is left
 * "ordered"/"in_progress", and vice versa.
 */
public class LabServiceImpl implements LabService {

    private final LabOrderDAO  labOrderDAO;
    private final LabResultDAO labResultDAO;

    public LabServiceImpl(LabOrderDAO labOrderDAO, LabResultDAO labResultDAO) {
        this.labOrderDAO  = labOrderDAO;
        this.labResultDAO = labResultDAO;
    }

    @Override
    public LabOrderDTO orderTest(CreateLabOrderDTO dto) throws Exception {
        ValidatorUtils.requireNonBlank(dto.getAppointmentId(), "appointmentId");
        ValidatorUtils.requireNonBlank(dto.getDoctorId(), "doctorId");
        String testName = ValidatorUtils.requireNonBlank(dto.getTestName(), "testName");
        ValidatorUtils.requireMaxLength(testName, 200, "testName");
        ValidatorUtils.requireNotPureNumeric(testName, "testName");
        dto.setTestName(testName);

        LabOrder order = LabOrderMapper.toEntity(dto);

        // A single INSERT is already atomic — no TransactionManager needed here.
        CacheService.evict(CacheKey.labOrdersByAppt(dto.getAppointmentId()));
        CacheService.evictByPattern(CacheKey.ALL_LAB);
        LabOrder saved = labOrderDAO.save(order);
        EventBus.publish(AppEventType.LAB_ORDER_CREATED, saved.getLabOrderId());
        return LabOrderMapper.toDTO(saved);
    }

    @Override
    public LabOrderDTO findOrderById(String labOrderId) throws Exception {
        ValidatorUtils.requireNonBlank(labOrderId, "labOrderId");
        Optional<LabOrderDTO> cached = CacheService.get(CacheKey.labOrder(labOrderId), LabOrderDTO.class);
        if (cached.isPresent()) return cached.get();

        LabOrder order = labOrderDAO.findById(labOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("LabOrder", labOrderId));
        LabOrderDTO result = LabOrderMapper.toDTO(order);
        CacheService.set(CacheKey.labOrder(labOrderId), result, CacheDomain.LAB);
        return result;
    }

    @Override
    public List<LabOrderDTO> findOrdersByAppointment(String appointmentId) throws Exception {
        ValidatorUtils.requireNonBlank(appointmentId, "appointmentId");
        Optional<List<LabOrderDTO>> cached = CacheService.get(
            CacheKey.labOrdersByAppt(appointmentId),
            new TypeReference<List<LabOrderDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<LabOrderDTO> dtos = new ArrayList<>();
        for (LabOrder order : labOrderDAO.findByAppointmentId(appointmentId)) {
            dtos.add(LabOrderMapper.toDTO(order));
        }
        CacheService.set(CacheKey.labOrdersByAppt(appointmentId), dtos, CacheDomain.LAB);
        return dtos;
    }

    @Override
    public LabResultDTO recordResult(CreateLabResultDTO dto) throws Exception {
        String labOrderId = ValidatorUtils.requireNonBlank(dto.getLabOrderId(), "labOrderId");

        LabOrder order = labOrderDAO.findById(labOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("LabOrder", labOrderId));
        if (labResultDAO.findByLabOrderId(labOrderId).isPresent()) {
            throw new ValidationException("labOrderId", "A result has already been recorded for this lab order.");
        }

        LabResult result = LabResultMapper.toEntity(dto);

        CacheService.evict(CacheKey.labResult(labOrderId));
        CacheService.evict(CacheKey.labOrder(labOrderId));
        CacheService.evict(CacheKey.labOrdersByAppt(order.getAppointmentId()));

        // Recording the result finalizes its order — both writes commit or roll back together.
        LabResult saved = TransactionManager.executeInTransaction(conn -> {
            LabResult r = labResultDAO.save(result, conn);
            labOrderDAO.updateStatus(labOrderId, LabOrderStatus.COMPLETED.getDbValue(), conn);
            return r;
        });

        EventBus.publish(AppEventType.LAB_RESULT_READY, saved.getLabResultId());
        EventBus.publish(AppEventType.LAB_ORDER_UPDATED, labOrderId);
        return LabResultMapper.toDTO(saved);
    }

    @Override
    public LabResultDTO findResultByOrder(String labOrderId) throws Exception {
        ValidatorUtils.requireNonBlank(labOrderId, "labOrderId");
        Optional<LabResultDTO> cached = CacheService.get(CacheKey.labResult(labOrderId), LabResultDTO.class);
        if (cached.isPresent()) return cached.get();

        LabResult result = labResultDAO.findByLabOrderId(labOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("LabResult", labOrderId));
        LabResultDTO dto = LabResultMapper.toDTO(result);
        CacheService.set(CacheKey.labResult(labOrderId), dto, CacheDomain.LAB);
        return dto;
    }

    @Override
    public void deleteOrder(String labOrderId) throws Exception {
        ValidatorUtils.requireNonBlank(labOrderId, "labOrderId");
        LabOrder order = labOrderDAO.findById(labOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("LabOrder", labOrderId));

        CacheService.evict(CacheKey.labOrder(labOrderId));
        CacheService.evict(CacheKey.labResult(labOrderId));
        CacheService.evict(CacheKey.labOrdersByAppt(order.getAppointmentId()));
        CacheService.evictByPattern(CacheKey.ALL_LAB);
        labOrderDAO.softDelete(labOrderId);
    }
}
