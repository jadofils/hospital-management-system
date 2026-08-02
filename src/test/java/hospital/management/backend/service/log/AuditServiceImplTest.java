package hospital.management.backend.service.log;

import hospital.management.backend.dao.log.interfaces.AuditLogDAO;
import hospital.management.backend.dto.log.AuditLogDTO;
import hospital.management.backend.model.user.AuditLog;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuditServiceImpl.record() publishes AppEventType.AUDIT_LOG_RECORDED through the static
 * EventBus after a successful save. As long as this test class does not itself register a
 * listener for that event type, EventBus.publish short-circuits before touching
 * Platform.runLater (see EventBus.publish — an empty/absent listener list returns
 * immediately), so no JavaFX toolkit needs to be initialised here.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditLogDAO auditLogDAO;

    private AuditServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuditServiceImpl(auditLogDAO);
    }

    private AuditLog savedLog(String id, String userId, String action, String table, String recordId) {
        AuditLog log = new AuditLog();
        log.setLogId(id);
        log.setUserId(userId);
        log.setAction(action);
        log.setTableAffected(table);
        log.setRecordId(recordId);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    // ── record ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("record throws IllegalArgumentException when action is blank")
    void record_throwsIllegalArgumentException_whenActionBlank() {
        String userId = UUID.randomUUID().toString();

        assertThrows(IllegalArgumentException.class,
                () -> service.record(userId, "  ", "patients", null));
        verifyNoInteractions(auditLogDAO);
    }

    @Test
    @DisplayName("record throws IllegalArgumentException when table is blank")
    void record_throwsIllegalArgumentException_whenTableBlank() {
        String userId = UUID.randomUUID().toString();

        assertThrows(IllegalArgumentException.class,
                () -> service.record(userId, "CREATE", " ", null));
        verifyNoInteractions(auditLogDAO);
    }

    @Test
    @DisplayName("record saves an audit log with the given fields and returns the mapped DTO")
    void record_savesAndReturnsDto() throws Exception {
        String userId = UUID.randomUUID().toString();
        String recordId = UUID.randomUUID().toString();
        String logId = UUID.randomUUID().toString();
        when(auditLogDAO.save(any(AuditLog.class)))
                .thenReturn(savedLog(logId, userId, "CREATE", "patients", recordId));

        AuditLogDTO result = service.record(userId, "CREATE", "patients", recordId);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDAO).save(captor.capture());
        AuditLog toSave = captor.getValue();
        assertEquals(userId, toSave.getUserId());
        assertEquals("CREATE", toSave.getAction());
        assertEquals("patients", toSave.getTableAffected());
        assertEquals(recordId, toSave.getRecordId());

        assertEquals(logId, result.getLogId());
        assertEquals("CREATE", result.getAction());
        assertEquals("patients", result.getTableAffected());
    }

    @Test
    @DisplayName("record allows a null userId — system-originated actions have no acting user")
    void record_allowsNullUserId() throws Exception {
        when(auditLogDAO.save(any(AuditLog.class)))
                .thenReturn(savedLog(UUID.randomUUID().toString(), null, "CREATE", "patients", null));

        AuditLogDTO result = service.record(null, "CREATE", "patients", null);

        assertNull(result.getUserId());
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll maps each DAO entity into a DTO while preserving pagination metadata")
    void findAll_mapsDtos() throws Exception {
        AuditLog entity = savedLog(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                "UPDATE", "doctors", UUID.randomUUID().toString());
        PageRequest request = CursorPagination.firstPage();
        PageResult<AuditLog> daoResult = CursorPagination.toResult(List.of(entity), request, AuditLog::getCreatedAt);
        when(auditLogDAO.findAll(request)).thenReturn(daoResult);

        PageResult<AuditLogDTO> result = service.findAll(request);

        assertEquals(1, result.getCount());
        assertEquals(entity.getLogId(), result.getItems().get(0).getLogId());
        assertEquals("UPDATE", result.getItems().get(0).getAction());
    }

    // ── findByUser ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUser maps every DAO entity for that user into a DTO")
    void findByUser_mapsDtos() throws Exception {
        String userId = UUID.randomUUID().toString();
        AuditLog entity = savedLog(UUID.randomUUID().toString(), userId, "DELETE", "invoices", null);
        when(auditLogDAO.findByUserId(userId)).thenReturn(List.of(entity));

        List<AuditLogDTO> result = service.findByUser(userId);

        assertEquals(1, result.size());
        assertEquals("DELETE", result.get(0).getAction());
        verify(auditLogDAO).findByUserId(userId);
    }

    @Test
    @DisplayName("findByUser returns an empty list when the DAO finds nothing")
    void findByUser_returnsEmptyList_whenNoneFound() throws Exception {
        String userId = UUID.randomUUID().toString();
        when(auditLogDAO.findByUserId(userId)).thenReturn(List.of());

        List<AuditLogDTO> result = service.findByUser(userId);

        assertTrue(result.isEmpty());
    }

    // ── purgeOlderThanDays ────────────────────────────────────────────────

    @Test
    @DisplayName("purgeOlderThanDays delegates to the DAO and returns the deleted row count")
    void purgeOlderThanDays_delegatesToDao() throws Exception {
        when(auditLogDAO.deleteOlderThanDays(90)).thenReturn(7);

        int result = service.purgeOlderThanDays(90);

        assertEquals(7, result);
        verify(auditLogDAO).deleteOlderThanDays(90);
    }
}
