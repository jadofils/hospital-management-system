package hospital.management.backend.service.lab;

import hospital.management.backend.config.db.TransactionManager;
import hospital.management.backend.dao.lab.interfaces.LabOrderDAO;
import hospital.management.backend.dao.lab.interfaces.LabResultDAO;
import hospital.management.backend.dto.lab.CreateLabOrderDTO;
import hospital.management.backend.dto.lab.CreateLabResultDTO;
import hospital.management.backend.dto.lab.LabOrderDTO;
import hospital.management.backend.dto.lab.LabResultDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.lab.LabOrder;
import hospital.management.backend.model.lab.LabResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Every id used here is a fresh random UUID rather than a fixed literal — findOrderById()/
 * findResultByOrder() read through a real, JVM-wide, static L1 in-process cache with no
 * reset hook exposed to tests, so a fixed id would risk one test's cached DTO leaking into
 * another test's assertions (see PatientServiceImplTest for the same rationale).
 *
 * recordResult() wraps its DAO writes in TransactionManager.executeInTransaction(...), a
 * static method that normally opens a real JDBC Connection — mocked here (via mockStatic,
 * same pattern as AuthServiceImplTest) to just invoke the work lambda directly with a stub
 * Connection, so these tests never touch a real database.
 */
@ExtendWith(MockitoExtension.class)
class LabServiceImplTest {

    @Mock private LabOrderDAO labOrderDAO;
    @Mock private LabResultDAO labResultDAO;

    private LabServiceImpl service;
    private MockedStatic<TransactionManager> transactionManagerMock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new LabServiceImpl(labOrderDAO, labResultDAO);

        transactionManagerMock = mockStatic(TransactionManager.class);
        transactionManagerMock
                .when(() -> TransactionManager.executeInTransaction(any(TransactionManager.TransactionalWork.class)))
                .thenAnswer(invocation -> {
                    TransactionManager.TransactionalWork<?> work = invocation.getArgument(0);
                    return work.execute(mock(Connection.class));
                });
    }

    @AfterEach
    void tearDown() {
        transactionManagerMock.close();
    }

    private LabOrder sampleOrder(String id) {
        LabOrder order = new LabOrder();
        order.setLabOrderId(id);
        order.setAppointmentId(UUID.randomUUID().toString());
        order.setDoctorId(UUID.randomUUID().toString());
        order.setTestName("Complete Blood Count");
        order.setStatus("ordered");
        order.setOrderedAt(LocalDateTime.now());
        return order;
    }

    private LabResult sampleResult(String id, String labOrderId) {
        LabResult result = new LabResult();
        result.setLabResultId(id);
        result.setLabOrderId(labOrderId);
        result.setResultValue("5.4");
        result.setUnit("x10^9/L");
        result.setIsAbnormal(false);
        result.setCreatedAt(LocalDateTime.now());
        return result;
    }

    // ── orderTest ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("orderTest throws IllegalArgumentException when appointmentId is blank")
    void orderTest_throwsIllegalArgumentException_whenAppointmentIdBlank() {
        CreateLabOrderDTO dto = new CreateLabOrderDTO("  ", UUID.randomUUID().toString(), "CBC");

        assertThrows(IllegalArgumentException.class, () -> service.orderTest(dto));
        verifyNoInteractions(labOrderDAO);
    }

    @Test
    @DisplayName("orderTest throws IllegalArgumentException when doctorId is blank")
    void orderTest_throwsIllegalArgumentException_whenDoctorIdBlank() {
        CreateLabOrderDTO dto = new CreateLabOrderDTO(UUID.randomUUID().toString(), " ", "CBC");

        assertThrows(IllegalArgumentException.class, () -> service.orderTest(dto));
        verifyNoInteractions(labOrderDAO);
    }

    @Test
    @DisplayName("orderTest throws IllegalArgumentException when testName is blank")
    void orderTest_throwsIllegalArgumentException_whenTestNameBlank() {
        CreateLabOrderDTO dto = new CreateLabOrderDTO(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), "");

        assertThrows(IllegalArgumentException.class, () -> service.orderTest(dto));
        verifyNoInteractions(labOrderDAO);
    }

    @Test
    @DisplayName("orderTest saves a new order defaulting its status to 'ordered'")
    void orderTest_savesOrder_whenValid() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        String doctorId = UUID.randomUUID().toString();
        CreateLabOrderDTO dto = new CreateLabOrderDTO(appointmentId, doctorId, "Complete Blood Count");
        when(labOrderDAO.save(any(LabOrder.class))).thenAnswer(inv -> {
            LabOrder o = inv.getArgument(0);
            o.setLabOrderId(UUID.randomUUID().toString());
            o.setOrderedAt(LocalDateTime.now());
            return o;
        });

        LabOrderDTO result = service.orderTest(dto);

        ArgumentCaptor<LabOrder> captor = ArgumentCaptor.forClass(LabOrder.class);
        verify(labOrderDAO).save(captor.capture());
        assertEquals("ordered", captor.getValue().getStatus());
        assertEquals("Complete Blood Count", result.getTestName());
        assertEquals(appointmentId, result.getAppointmentId());
        assertEquals(doctorId, result.getDoctorId());
    }

    // ── findOrderById ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findOrderById throws IllegalArgumentException when labOrderId is blank")
    void findOrderById_throwsIllegalArgumentException_whenBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.findOrderById(" "));
        verifyNoInteractions(labOrderDAO);
    }

    @Test
    @DisplayName("findOrderById returns a mapped DTO when the DAO finds a matching order")
    void findOrderById_returnsMappedDto_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(labOrderDAO.findById(id)).thenReturn(Optional.of(sampleOrder(id)));

        LabOrderDTO dto = service.findOrderById(id);

        assertEquals(id, dto.getLabOrderId());
        assertEquals("Complete Blood Count", dto.getTestName());
        verify(labOrderDAO).findById(id);
    }

    @Test
    @DisplayName("findOrderById throws ResourceNotFoundException when the DAO finds nothing")
    void findOrderById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(labOrderDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findOrderById(id));
    }

    // ── findOrdersByAppointment ───────────────────────────────────────────

    @Test
    @DisplayName("findOrdersByAppointment throws IllegalArgumentException when appointmentId is blank")
    void findOrdersByAppointment_throwsIllegalArgumentException_whenBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.findOrdersByAppointment(""));
        verifyNoInteractions(labOrderDAO);
    }

    @Test
    @DisplayName("findOrdersByAppointment maps every DAO order to a DTO")
    void findOrdersByAppointment_mapsEveryOrder() throws Exception {
        String appointmentId = UUID.randomUUID().toString();
        LabOrder order1 = sampleOrder(UUID.randomUUID().toString());
        order1.setAppointmentId(appointmentId);
        LabOrder order2 = sampleOrder(UUID.randomUUID().toString());
        order2.setAppointmentId(appointmentId);
        order2.setTestName("Lipid Panel");
        when(labOrderDAO.findByAppointmentId(appointmentId)).thenReturn(List.of(order1, order2));

        List<LabOrderDTO> result = service.findOrdersByAppointment(appointmentId);

        assertEquals(2, result.size());
        assertEquals("Lipid Panel", result.get(1).getTestName());
    }

    // ── recordResult ──────────────────────────────────────────────────────

    @Test
    @DisplayName("recordResult throws IllegalArgumentException when labOrderId is blank")
    void recordResult_throwsIllegalArgumentException_whenLabOrderIdBlank() {
        CreateLabResultDTO dto = new CreateLabResultDTO(" ", "5.4", "x10^9/L", null, false, null);

        assertThrows(IllegalArgumentException.class, () -> service.recordResult(dto));
        verifyNoInteractions(labOrderDAO, labResultDAO);
    }

    @Test
    @DisplayName("recordResult throws ResourceNotFoundException when the lab order doesn't exist")
    void recordResult_throwsResourceNotFoundException_whenOrderMissing() throws Exception {
        String labOrderId = UUID.randomUUID().toString();
        when(labOrderDAO.findById(labOrderId)).thenReturn(Optional.empty());
        CreateLabResultDTO dto = new CreateLabResultDTO(labOrderId, "5.4", "x10^9/L", null, false, null);

        assertThrows(ResourceNotFoundException.class, () -> service.recordResult(dto));
        verify(labResultDAO, never()).save(any(), any());
    }

    @Test
    @DisplayName("recordResult throws ValidationException when a result already exists for the order")
    void recordResult_throwsValidationException_whenResultAlreadyRecorded() throws Exception {
        String labOrderId = UUID.randomUUID().toString();
        when(labOrderDAO.findById(labOrderId)).thenReturn(Optional.of(sampleOrder(labOrderId)));
        when(labResultDAO.findByLabOrderId(labOrderId))
                .thenReturn(Optional.of(sampleResult(UUID.randomUUID().toString(), labOrderId)));
        CreateLabResultDTO dto = new CreateLabResultDTO(labOrderId, "5.4", "x10^9/L", null, false, null);

        assertThrows(ValidationException.class, () -> service.recordResult(dto));
        verify(labResultDAO, never()).save(any(), any());
        verify(labOrderDAO, never()).updateStatus(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("recordResult saves the result and finalizes the order's status to 'completed' in one transaction")
    void recordResult_savesResultAndCompletesOrder() throws Exception {
        String labOrderId = UUID.randomUUID().toString();
        LabOrder order = sampleOrder(labOrderId);
        when(labOrderDAO.findById(labOrderId)).thenReturn(Optional.of(order));
        when(labResultDAO.findByLabOrderId(labOrderId)).thenReturn(Optional.empty());
        when(labResultDAO.save(any(LabResult.class), any(Connection.class))).thenAnswer(inv -> {
            LabResult r = inv.getArgument(0);
            r.setLabResultId(UUID.randomUUID().toString());
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });
        CreateLabResultDTO dto = new CreateLabResultDTO(labOrderId, "5.4", "x10^9/L", "4.0-11.0", false, null);

        LabResultDTO result = service.recordResult(dto);

        assertEquals(labOrderId, result.getLabOrderId());
        assertEquals("5.4", result.getResultValue());
        verify(labResultDAO).save(any(LabResult.class), any(Connection.class));
        verify(labOrderDAO).updateStatus(eq(labOrderId), eq("completed"), any(Connection.class));
    }

    // ── findResultByOrder ─────────────────────────────────────────────────

    @Test
    @DisplayName("findResultByOrder throws IllegalArgumentException when labOrderId is blank")
    void findResultByOrder_throwsIllegalArgumentException_whenBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.findResultByOrder(""));
        verifyNoInteractions(labResultDAO);
    }

    @Test
    @DisplayName("findResultByOrder returns a mapped DTO when the DAO finds a matching result")
    void findResultByOrder_returnsMappedDto_whenFound() throws Exception {
        String labOrderId = UUID.randomUUID().toString();
        when(labResultDAO.findByLabOrderId(labOrderId))
                .thenReturn(Optional.of(sampleResult(UUID.randomUUID().toString(), labOrderId)));

        LabResultDTO dto = service.findResultByOrder(labOrderId);

        assertEquals(labOrderId, dto.getLabOrderId());
        assertEquals("5.4", dto.getResultValue());
    }

    @Test
    @DisplayName("findResultByOrder throws ResourceNotFoundException when no result has been recorded")
    void findResultByOrder_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String labOrderId = UUID.randomUUID().toString();
        when(labResultDAO.findByLabOrderId(labOrderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findResultByOrder(labOrderId));
    }

    // ── deleteOrder ───────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteOrder throws IllegalArgumentException when labOrderId is blank")
    void deleteOrder_throwsIllegalArgumentException_whenBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteOrder(" "));
        verifyNoInteractions(labOrderDAO);
    }

    @Test
    @DisplayName("deleteOrder throws ResourceNotFoundException when the order doesn't exist")
    void deleteOrder_throwsResourceNotFoundException_whenMissing() throws Exception {
        String id = UUID.randomUUID().toString();
        when(labOrderDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteOrder(id));
        verify(labOrderDAO, never()).softDelete(anyString());
    }

    @Test
    @DisplayName("deleteOrder delegates to the DAO's soft-delete when the order exists")
    void deleteOrder_softDeletes_whenOrderExists() throws Exception {
        String id = UUID.randomUUID().toString();
        when(labOrderDAO.findById(id)).thenReturn(Optional.of(sampleOrder(id)));

        service.deleteOrder(id);

        verify(labOrderDAO).softDelete(id);
    }
}
