package hospital.management.backend.service.pharmacy;

import com.fasterxml.jackson.core.type.TypeReference;
import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.config.db.TransactionManager;
import hospital.management.backend.dao.pharmacy.interfaces.PrescriptionDAO;
import hospital.management.backend.dao.pharmacy.interfaces.PrescriptionItemDAO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionDTO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionItemDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionItemDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.pharmacy.PrescriptionItemMapper;
import hospital.management.backend.mapper.pharmacy.PrescriptionMapper;
import hospital.management.backend.model.pharmacy.Prescription;
import hospital.management.backend.model.pharmacy.PrescriptionItem;
import hospital.management.backend.service.pharmacy.interfaces.PrescriptionService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A prescription header is never persisted without at least one line item (and
 * vice versa) — {@link #issue(CreatePrescriptionDTO)} wraps both inserts in a single
 * {@link TransactionManager#executeInTransaction} block so the write is atomic.
 */
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionDAO     prescriptionDAO;
    private final PrescriptionItemDAO itemDAO;

    public PrescriptionServiceImpl(PrescriptionDAO prescriptionDAO, PrescriptionItemDAO itemDAO) {
        this.prescriptionDAO = prescriptionDAO;
        this.itemDAO         = itemDAO;
    }

    @Override
    public PrescriptionDTO issue(CreatePrescriptionDTO dto) throws Exception {
        String appointmentId = ValidatorUtils.requireNonBlank(dto.getAppointmentId(), "appointmentId");
        ValidatorUtils.requireValidUuid(appointmentId, "appointmentId");

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new ValidationException("items", "A prescription must include at least one item.");
        }
        for (CreatePrescriptionItemDTO itemDto : dto.getItems()) {
            ValidatorUtils.requireNonBlank(itemDto.getMedicationId(), "medicationId");
            if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new ValidationException("quantity", "Quantity must be greater than zero.");
            }
            if (itemDto.getDosage() != null) {
                ValidatorUtils.requireMaxLength(itemDto.getDosage(), 200, "dosage");
            }
            if (itemDto.getInstructions() != null) {
                ValidatorUtils.requireMaxLength(itemDto.getInstructions(), 500, "instructions");
            }
        }

        Prescription prescription = PrescriptionMapper.toEntity(dto);
        if (prescription.getDateIssued() == null) {
            prescription.setDateIssued(LocalDate.now());
        }

        // Opening the header row and every line item succeed or fail together —
        // a prescription with no items (or an orphaned item) would be a real bug.
        CacheService.evict(CacheKey.prescriptionByAppt(appointmentId));
        CacheService.evictByPattern(CacheKey.ALL_PHARMACY);

        List<PrescriptionItemDTO> itemDTOs = new ArrayList<>();
        Prescription saved = TransactionManager.executeInTransaction(conn -> {
            Prescription savedPrescription = prescriptionDAO.save(prescription, conn);
            for (CreatePrescriptionItemDTO itemDto : dto.getItems()) {
                PrescriptionItem item = PrescriptionItemMapper.toEntity(savedPrescription.getPrescriptionId(), itemDto);
                PrescriptionItem savedItem = itemDAO.save(item, conn);
                itemDTOs.add(PrescriptionItemMapper.toDTO(savedItem));
            }
            return savedPrescription;
        });

        PrescriptionDTO result = PrescriptionMapper.toDTO(saved);
        result.setItems(itemDTOs);
        CacheService.set(CacheKey.prescription(saved.getPrescriptionId()), result, CacheDomain.PRESCRIPTION);
        EventBus.publish(AppEventType.PRESCRIPTION_CREATED, saved.getPrescriptionId());
        return result;
    }

    @Override
    public PrescriptionDTO findById(String prescriptionId) throws Exception {
        Optional<PrescriptionDTO> cached = CacheService.get(CacheKey.prescription(prescriptionId), PrescriptionDTO.class);
        if (cached.isPresent()) return cached.get();

        Prescription prescription = prescriptionDAO.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", prescriptionId));
        PrescriptionDTO dto = toDTOWithItems(prescription);
        CacheService.set(CacheKey.prescription(prescriptionId), dto, CacheDomain.PRESCRIPTION);
        return dto;
    }

    @Override
    public PrescriptionDTO findByAppointment(String appointmentId) throws Exception {
        Optional<PrescriptionDTO> cached = CacheService.get(CacheKey.prescriptionByAppt(appointmentId), PrescriptionDTO.class);
        if (cached.isPresent()) return cached.get();

        Prescription prescription = prescriptionDAO.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", appointmentId));
        PrescriptionDTO dto = toDTOWithItems(prescription);
        CacheService.set(CacheKey.prescriptionByAppt(appointmentId), dto, CacheDomain.PRESCRIPTION);
        return dto;
    }

    @Override
    public List<PrescriptionDTO> findByPatient(String patientId) throws Exception {
        Optional<List<PrescriptionDTO>> cached = CacheService.get(
                CacheKey.prescriptionsByPatient(patientId),
                new TypeReference<List<PrescriptionDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<PrescriptionDTO> dtos = new ArrayList<>();
        for (Prescription prescription : prescriptionDAO.findByPatientId(patientId)) {
            dtos.add(toDTOWithItems(prescription));
        }
        CacheService.set(CacheKey.prescriptionsByPatient(patientId), dtos, CacheDomain.PRESCRIPTION);
        return dtos;
    }

    @Override
    public void delete(String prescriptionId) throws Exception {
        Prescription prescription = prescriptionDAO.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", prescriptionId));

        CacheService.evict(CacheKey.prescription(prescriptionId));
        CacheService.evict(CacheKey.prescriptionByAppt(prescription.getAppointmentId()));
        CacheService.evictByPattern(CacheKey.ALL_PHARMACY);

        prescriptionDAO.softDelete(prescriptionId);
        EventBus.publish(AppEventType.PRESCRIPTION_UPDATED, prescriptionId);
    }

    private PrescriptionDTO toDTOWithItems(Prescription prescription) throws Exception {
        PrescriptionDTO dto = PrescriptionMapper.toDTO(prescription);
        List<PrescriptionItemDTO> items = new ArrayList<>();
        for (PrescriptionItem item : itemDAO.findByPrescriptionId(prescription.getPrescriptionId())) {
            items.add(PrescriptionItemMapper.toDTO(item));
        }
        dto.setItems(items);
        return dto;
    }
}
