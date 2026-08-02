package hospital.management.backend.service.pharmacy;

import hospital.management.backend.dao.pharmacy.interfaces.MedicalInventoryDAO;
import hospital.management.backend.dao.pharmacy.interfaces.MedicationDAO;
import hospital.management.backend.dto.pharmacy.CreateMedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.CreateMedicationDTO;
import hospital.management.backend.dto.pharmacy.MedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.MedicationDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.pharmacy.MedicalInventory;
import hospital.management.backend.model.pharmacy.Medication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Every id used here is a fresh random UUID rather than a fixed literal —
 * findMedicationById()/CacheService.get() reads through a real, JVM-wide, static L1
 * in-process cache with no reset hook exposed to tests, so a fixed id would risk one
 * test's cached DTO leaking into another test's assertions (see PatientServiceImplTest
 * for the same rationale).
 */
@ExtendWith(MockitoExtension.class)
class PharmacyServiceImplTest {

    @Mock private MedicationDAO medicationDAO;
    @Mock private MedicalInventoryDAO inventoryDAO;

    private PharmacyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PharmacyServiceImpl(medicationDAO, inventoryDAO);
    }

    private Medication sampleMedication(String id) {
        Medication m = new Medication();
        m.setMedicationId(id);
        m.setName("Paracetamol");
        m.setGenericName("Acetaminophen");
        m.setForm("tablet");
        m.setUnitPrice(new BigDecimal("9.99"));
        return m;
    }

    private MedicalInventory sampleInventory(String id, String medicationId, int quantity, int reorderLevel) {
        MedicalInventory i = new MedicalInventory();
        i.setInventoryId(id);
        i.setMedicationId(medicationId);
        i.setBatchNumber("BATCH-001");
        i.setExpiryDate(LocalDate.now().plusYears(1));
        i.setQuantityInStock(quantity);
        i.setReorderLevel(reorderLevel);
        i.setSupplier("Acme Pharma");
        return i;
    }

    // ── addMedication ─────────────────────────────────────────────────────

    @Test
    @DisplayName("addMedication throws IllegalArgumentException when name is blank")
    void addMedication_throwsIllegalArgumentException_whenNameBlank() {
        CreateMedicationDTO dto = new CreateMedicationDTO("  ", null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.addMedication(dto));
        verifyNoInteractions(medicationDAO);
    }

    @Test
    @DisplayName("addMedication throws ValidationException when unit price is negative")
    void addMedication_throwsValidationException_whenUnitPriceNegative() {
        CreateMedicationDTO dto = new CreateMedicationDTO("Paracetamol", null, null, new BigDecimal("-1.00"));

        assertThrows(ValidationException.class, () -> service.addMedication(dto));
        verifyNoInteractions(medicationDAO);
    }

    @Test
    @DisplayName("addMedication saves a new medication when everything is valid")
    void addMedication_savesMedication_whenValid() throws Exception {
        CreateMedicationDTO dto = new CreateMedicationDTO("Paracetamol", "Acetaminophen", "tablet", new BigDecimal("9.99"));
        when(medicationDAO.save(any(Medication.class))).thenAnswer(inv -> {
            Medication m = inv.getArgument(0);
            m.setMedicationId(UUID.randomUUID().toString());
            return m;
        });

        MedicationDTO result = service.addMedication(dto);

        ArgumentCaptor<Medication> captor = ArgumentCaptor.forClass(Medication.class);
        verify(medicationDAO).save(captor.capture());
        assertEquals("Paracetamol", captor.getValue().getName());
        assertEquals("Paracetamol", result.getName());
        assertEquals(0, new BigDecimal("9.99").compareTo(result.getUnitPrice()));
    }

    @Test
    @DisplayName("addMedication allows a null unit price (no negativity check applies)")
    void addMedication_allowsNullUnitPrice() throws Exception {
        CreateMedicationDTO dto = new CreateMedicationDTO("Paracetamol", null, null, null);
        when(medicationDAO.save(any(Medication.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.addMedication(dto));
        verify(medicationDAO).save(any(Medication.class));
    }

    // ── findMedicationById ────────────────────────────────────────────────

    @Test
    @DisplayName("findMedicationById returns a mapped DTO when the DAO finds a matching medication")
    void findMedicationById_returnsMappedDto_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(medicationDAO.findById(id)).thenReturn(Optional.of(sampleMedication(id)));

        MedicationDTO dto = service.findMedicationById(id);

        assertEquals(id, dto.getMedicationId());
        assertEquals("Paracetamol", dto.getName());
        verify(medicationDAO).findById(id);
    }

    @Test
    @DisplayName("findMedicationById throws ResourceNotFoundException when the DAO finds nothing")
    void findMedicationById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(medicationDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findMedicationById(id));
    }

    // ── findAllMedications ────────────────────────────────────────────────

    @Test
    @DisplayName("findAllMedications maps every DAO medication to a DTO")
    void findAllMedications_mapsEveryMedication() throws Exception {
        when(medicationDAO.findAll()).thenReturn(List.of(
                sampleMedication(UUID.randomUUID().toString()),
                sampleMedication(UUID.randomUUID().toString())));

        List<MedicationDTO> result = service.findAllMedications();

        assertEquals(2, result.size());
    }

    // ── addStock ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("addStock throws IllegalArgumentException when medicationId is blank")
    void addStock_throwsIllegalArgumentException_whenMedicationIdBlank() {
        CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO("  ", "B1", LocalDate.now(), 10, 5, "Acme");

        assertThrows(IllegalArgumentException.class, () -> service.addStock(dto));
        verifyNoInteractions(medicationDAO, inventoryDAO);
    }

    @Test
    @DisplayName("addStock throws ValidationException when expiryDate is missing")
    void addStock_throwsValidationException_whenExpiryDateMissing() {
        String medicationId = UUID.randomUUID().toString();
        CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(medicationId, "B1", null, 10, 5, "Acme");

        assertThrows(ValidationException.class, () -> service.addStock(dto));
        verifyNoInteractions(inventoryDAO);
    }

    @Test
    @DisplayName("addStock throws ValidationException when quantityInStock is negative")
    void addStock_throwsValidationException_whenQuantityNegative() {
        String medicationId = UUID.randomUUID().toString();
        CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(medicationId, "B1", LocalDate.now(), -1, 5, "Acme");

        assertThrows(ValidationException.class, () -> service.addStock(dto));
        verifyNoInteractions(inventoryDAO);
    }

    @Test
    @DisplayName("addStock throws ValidationException when reorderLevel is negative")
    void addStock_throwsValidationException_whenReorderLevelNegative() {
        String medicationId = UUID.randomUUID().toString();
        CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(medicationId, "B1", LocalDate.now(), 10, -1, "Acme");

        assertThrows(ValidationException.class, () -> service.addStock(dto));
        verifyNoInteractions(inventoryDAO);
    }

    @Test
    @DisplayName("addStock throws ResourceNotFoundException when the medication it belongs to doesn't exist")
    void addStock_throwsResourceNotFoundException_whenMedicationMissing() throws Exception {
        String medicationId = UUID.randomUUID().toString();
        CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(medicationId, "B1", LocalDate.now(), 10, 5, "Acme");
        when(medicationDAO.findById(medicationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.addStock(dto));
        verify(inventoryDAO, never()).save(any());
    }

    @Test
    @DisplayName("addStock saves a new batch when the medication exists and everything is valid")
    void addStock_savesBatch_whenValid() throws Exception {
        String medicationId = UUID.randomUUID().toString();
        CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(medicationId, "B1", LocalDate.now(), 100, 10, "Acme");
        when(medicationDAO.findById(medicationId)).thenReturn(Optional.of(sampleMedication(medicationId)));
        when(inventoryDAO.save(any(MedicalInventory.class))).thenAnswer(inv -> {
            MedicalInventory i = inv.getArgument(0);
            i.setInventoryId(UUID.randomUUID().toString());
            return i;
        });

        MedicalInventoryDTO result = service.addStock(dto);

        assertEquals(medicationId, result.getMedicationId());
        assertEquals(100, result.getQuantityInStock());
    }

    // ── findStockByMedication / findLowStock ──────────────────────────────

    @Test
    @DisplayName("findStockByMedication maps every DAO inventory batch to a DTO")
    void findStockByMedication_mapsEveryBatch() throws Exception {
        String medicationId = UUID.randomUUID().toString();
        when(inventoryDAO.findByMedicationId(medicationId)).thenReturn(List.of(
                sampleInventory(UUID.randomUUID().toString(), medicationId, 100, 10)));

        List<MedicalInventoryDTO> result = service.findStockByMedication(medicationId);

        assertEquals(1, result.size());
        assertEquals(medicationId, result.get(0).getMedicationId());
    }

    @Test
    @DisplayName("findLowStock maps every DAO low-stock batch to a DTO")
    void findLowStock_mapsEveryBatch() throws Exception {
        String medicationId = UUID.randomUUID().toString();
        when(inventoryDAO.findLowStock()).thenReturn(List.of(
                sampleInventory(UUID.randomUUID().toString(), medicationId, 2, 10)));

        List<MedicalInventoryDTO> result = service.findLowStock();

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getQuantityInStock());
    }

    // ── updateStock ───────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStock throws ResourceNotFoundException when the inventory batch doesn't exist")
    void updateStock_throwsResourceNotFoundException_whenMissing() throws Exception {
        String inventoryId = UUID.randomUUID().toString();
        when(inventoryDAO.findById(inventoryId)).thenReturn(Optional.empty());
        CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(null, null, null, 50, null, null);

        assertThrows(ResourceNotFoundException.class, () -> service.updateStock(inventoryId, dto));
    }

    @Test
    @DisplayName("updateStock throws ValidationException when the new quantityInStock is negative")
    void updateStock_throwsValidationException_whenQuantityNegative() throws Exception {
        String inventoryId = UUID.randomUUID().toString();
        String medicationId = UUID.randomUUID().toString();
        when(inventoryDAO.findById(inventoryId)).thenReturn(Optional.of(sampleInventory(inventoryId, medicationId, 100, 10)));
        CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(null, null, null, -5, null, null);

        assertThrows(ValidationException.class, () -> service.updateStock(inventoryId, dto));
        verify(inventoryDAO, never()).update(any());
    }

    @Test
    @DisplayName("updateStock throws ValidationException when the new reorderLevel is negative")
    void updateStock_throwsValidationException_whenReorderLevelNegative() throws Exception {
        String inventoryId = UUID.randomUUID().toString();
        String medicationId = UUID.randomUUID().toString();
        when(inventoryDAO.findById(inventoryId)).thenReturn(Optional.of(sampleInventory(inventoryId, medicationId, 100, 10)));
        CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(null, null, null, null, -5, null);

        assertThrows(ValidationException.class, () -> service.updateStock(inventoryId, dto));
        verify(inventoryDAO, never()).update(any());
    }

    @Test
    @DisplayName("updateStock only overwrites the fields present on the DTO, leaving the rest untouched")
    void updateStock_onlyOverwritesProvidedFields() throws Exception {
        String inventoryId = UUID.randomUUID().toString();
        String medicationId = UUID.randomUUID().toString();
        MedicalInventory existing = sampleInventory(inventoryId, medicationId, 100, 10);
        when(inventoryDAO.findById(inventoryId)).thenReturn(Optional.of(existing));
        when(inventoryDAO.update(any(MedicalInventory.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(null, null, null, 40, null, null);
        MedicalInventoryDTO result = service.updateStock(inventoryId, dto);

        assertEquals(40, result.getQuantityInStock());
        assertEquals("BATCH-001", result.getBatchNumber());
        assertEquals(10, result.getReorderLevel());
        assertEquals("Acme Pharma", result.getSupplier());
    }

    @Test
    @DisplayName("updateStock persists the merged batch via the DAO")
    void updateStock_persistsMergedBatch() throws Exception {
        String inventoryId = UUID.randomUUID().toString();
        String medicationId = UUID.randomUUID().toString();
        MedicalInventory existing = sampleInventory(inventoryId, medicationId, 100, 10);
        when(inventoryDAO.findById(inventoryId)).thenReturn(Optional.of(existing));
        when(inventoryDAO.update(any(MedicalInventory.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(null, "NEW-BATCH", null, null, null, "New Supplier");
        service.updateStock(inventoryId, dto);

        ArgumentCaptor<MedicalInventory> captor = ArgumentCaptor.forClass(MedicalInventory.class);
        verify(inventoryDAO).update(captor.capture());
        assertEquals("NEW-BATCH", captor.getValue().getBatchNumber());
        assertEquals("New Supplier", captor.getValue().getSupplier());
    }
}
