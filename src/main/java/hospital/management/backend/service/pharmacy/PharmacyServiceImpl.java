package hospital.management.backend.service.pharmacy;

import com.fasterxml.jackson.core.type.TypeReference;
import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.pharmacy.interfaces.MedicalInventoryDAO;
import hospital.management.backend.dao.pharmacy.interfaces.MedicationDAO;
import hospital.management.backend.dto.pharmacy.CreateMedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.CreateMedicationDTO;
import hospital.management.backend.dto.pharmacy.MedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.MedicationDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.pharmacy.MedicalInventoryMapper;
import hospital.management.backend.mapper.pharmacy.MedicationMapper;
import hospital.management.backend.model.pharmacy.MedicalInventory;
import hospital.management.backend.model.pharmacy.Medication;
import hospital.management.backend.service.pharmacy.interfaces.PharmacyService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PharmacyServiceImpl implements PharmacyService {

    private final MedicationDAO       medicationDAO;
    private final MedicalInventoryDAO inventoryDAO;

    public PharmacyServiceImpl(MedicationDAO medicationDAO, MedicalInventoryDAO inventoryDAO) {
        this.medicationDAO = medicationDAO;
        this.inventoryDAO  = inventoryDAO;
    }

    // ── Medication ───────────────────────────────────────────────────────────

    @Override
    public MedicationDTO addMedication(CreateMedicationDTO dto) throws Exception {
        String name = ValidatorUtils.requireNonBlank(dto.getName(), "name");
        ValidatorUtils.requireMaxLength(name, 200, "name");
        ValidatorUtils.requireNonBlank(dto.getForm(), "form");
        if (dto.getGenericName() != null) {
            ValidatorUtils.requireMaxLength(dto.getGenericName(), 200, "genericName");
        }
        if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("unitPrice", "Unit price must be greater than zero.");
        }

        // Single INSERT is already atomic — no TransactionManager needed here.
        CacheService.evict(CacheKey.medicationList());
        Medication saved = medicationDAO.save(MedicationMapper.toEntity(dto));
        EventBus.publish(AppEventType.MEDICATION_CREATED, saved.getMedicationId());
        return MedicationMapper.toDTO(saved);
    }

    @Override
    public MedicationDTO findMedicationById(String medicationId) throws Exception {
        Optional<MedicationDTO> cached = CacheService.get(CacheKey.medication(medicationId), MedicationDTO.class);
        if (cached.isPresent()) return cached.get();

        Medication medication = medicationDAO.findById(medicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Medication", medicationId));
        MedicationDTO dto = MedicationMapper.toDTO(medication);
        CacheService.set(CacheKey.medication(medicationId), dto, CacheDomain.PHARMACY);
        return dto;
    }

    @Override
    public List<MedicationDTO> findAllMedications() throws Exception {
        Optional<List<MedicationDTO>> cached = CacheService.get(
            CacheKey.medicationList(),
            new TypeReference<List<MedicationDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<MedicationDTO> dtos = new ArrayList<>();
        for (Medication medication : medicationDAO.findAll()) dtos.add(MedicationMapper.toDTO(medication));
        CacheService.set(CacheKey.medicationList(), dtos, CacheDomain.PHARMACY);
        return dtos;
    }

    // ── Medical inventory ────────────────────────────────────────────────────

    @Override
    public MedicalInventoryDTO addStock(CreateMedicalInventoryDTO dto) throws Exception {
        String medicationId = ValidatorUtils.requireNonBlank(dto.getMedicationId(), "medicationId");
        ValidatorUtils.requireNonBlank(dto.getBatchNumber(), "batchNumber");
        if (dto.getExpiryDate() == null) {
            throw new ValidationException("expiryDate", "Expiry date is required.");
        }
        if (dto.getQuantityInStock() == null || dto.getQuantityInStock() < 0) {
            throw new ValidationException("quantityInStock", "Quantity in stock must be a non-negative number.");
        }
        if (dto.getReorderLevel() != null && dto.getReorderLevel() < 0) {
            throw new ValidationException("reorderLevel", "Reorder level must not be negative.");
        }
        // Fail fast if the medication this batch belongs to doesn't exist.
        medicationDAO.findById(medicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Medication", medicationId));

        CacheService.evict(CacheKey.inventory(medicationId));
        CacheService.evict(CacheKey.lowStock());
        MedicalInventory saved = inventoryDAO.save(MedicalInventoryMapper.toEntity(dto));
        EventBus.publish(AppEventType.INVENTORY_UPDATED, saved.getInventoryId());
        if (isLowStock(saved)) {
            EventBus.publish(AppEventType.INVENTORY_LOW_STOCK, saved.getInventoryId());
        }
        return MedicalInventoryMapper.toDTO(saved);
    }

    @Override
    public List<MedicalInventoryDTO> findStockByMedication(String medicationId) throws Exception {
        Optional<List<MedicalInventoryDTO>> cached = CacheService.get(
            CacheKey.inventory(medicationId),
            new TypeReference<List<MedicalInventoryDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<MedicalInventoryDTO> dtos = new ArrayList<>();
        for (MedicalInventory inventory : inventoryDAO.findByMedicationId(medicationId)) {
            dtos.add(MedicalInventoryMapper.toDTO(inventory));
        }
        CacheService.set(CacheKey.inventory(medicationId), dtos, CacheDomain.PHARMACY);
        return dtos;
    }

    @Override
    public List<MedicalInventoryDTO> findLowStock() throws Exception {
        Optional<List<MedicalInventoryDTO>> cached = CacheService.get(
            CacheKey.lowStock(),
            new TypeReference<List<MedicalInventoryDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<MedicalInventoryDTO> dtos = new ArrayList<>();
        for (MedicalInventory inventory : inventoryDAO.findLowStock()) {
            dtos.add(MedicalInventoryMapper.toDTO(inventory));
        }
        // This is a fast-moving, "live" query (stock changes on every dispense),
        // so it's cached under CacheDomain.PHARMACY's TTL but evicted eagerly on
        // every inventory write below — the cache mainly absorbs read bursts
        // between writes rather than surviving long unattended.
        CacheService.set(CacheKey.lowStock(), dtos, CacheDomain.PHARMACY);
        return dtos;
    }

    @Override
    public MedicalInventoryDTO updateStock(String inventoryId, CreateMedicalInventoryDTO dto) throws Exception {
        MedicalInventory inventory = inventoryDAO.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalInventory", inventoryId));
        boolean wasLowStock = isLowStock(inventory);

        if (dto.getQuantityInStock() != null && dto.getQuantityInStock() < 0) {
            throw new ValidationException("quantityInStock", "Quantity in stock must not be negative.");
        }
        if (dto.getReorderLevel() != null && dto.getReorderLevel() < 0) {
            throw new ValidationException("reorderLevel", "Reorder level must not be negative.");
        }

        if (dto.getBatchNumber() != null) inventory.setBatchNumber(dto.getBatchNumber());
        if (dto.getExpiryDate() != null) inventory.setExpiryDate(dto.getExpiryDate());
        if (dto.getQuantityInStock() != null) inventory.setQuantityInStock(dto.getQuantityInStock());
        if (dto.getReorderLevel() != null) inventory.setReorderLevel(dto.getReorderLevel());
        if (dto.getSupplier() != null) inventory.setSupplier(dto.getSupplier());

        CacheService.evict(CacheKey.inventory(inventory.getMedicationId()));
        CacheService.evict(CacheKey.lowStock());
        MedicalInventory saved = inventoryDAO.update(inventory);
        EventBus.publish(AppEventType.INVENTORY_UPDATED, saved.getInventoryId());

        // Only fire the low-stock alert on the transition into low stock, not on
        // every update to a batch that was already below its reorder level —
        // otherwise every unrelated edit (e.g. changing the supplier) would
        // re-trigger the alert.
        boolean isNowLowStock = isLowStock(saved);
        if (isNowLowStock && !wasLowStock) {
            EventBus.publish(AppEventType.INVENTORY_LOW_STOCK, saved.getInventoryId());
        }
        return MedicalInventoryMapper.toDTO(saved);
    }

    private boolean isLowStock(MedicalInventory inventory) {
        return inventory.getQuantityInStock() != null
            && inventory.getReorderLevel() != null
            && inventory.getQuantityInStock() <= inventory.getReorderLevel();
    }
}
