package hospital.management.backend.service.log;

import hospital.management.backend.dao.log.interfaces.SystemLogDAO;
import hospital.management.backend.dto.log.SystemLogDTO;
import hospital.management.backend.model.user.SystemLog;
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
 * SystemLogServiceImpl.log() publishes AppEventType.SYSTEM_LOG_RECORDED through the static
 * EventBus after a successful save. As long as this test class does not itself register a
 * listener for that event type, EventBus.publish short-circuits before touching
 * Platform.runLater (an empty/absent listener list returns immediately), so no JavaFX
 * toolkit needs to be initialised here.
 */
@ExtendWith(MockitoExtension.class)
class SystemLogServiceImplTest {

    @Mock
    private SystemLogDAO systemLogDAO;

    private SystemLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SystemLogServiceImpl(systemLogDAO);
    }

    private SystemLog savedLog(String id, String level, String source, String message, String userId) {
        SystemLog log = new SystemLog();
        log.setLogId(id);
        log.setLogLevel(level);
        log.setSource(source);
        log.setMessage(message);
        log.setUserId(userId);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    // ── log ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("log throws IllegalArgumentException when level is blank")
    void log_throwsIllegalArgumentException_whenLevelBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> service.log(" ", "SchedulerJob", "message", null));
        verifyNoInteractions(systemLogDAO);
    }

    @Test
    @DisplayName("log throws IllegalArgumentException when source is blank")
    void log_throwsIllegalArgumentException_whenSourceBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> service.log("INFO", "", "message", null));
        verifyNoInteractions(systemLogDAO);
    }

    @Test
    @DisplayName("log throws IllegalArgumentException when message is blank")
    void log_throwsIllegalArgumentException_whenMessageBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> service.log("INFO", "SchedulerJob", "   ", null));
        verifyNoInteractions(systemLogDAO);
    }

    @Test
    @DisplayName("log saves a system log entry with the given fields and returns the mapped DTO")
    void log_savesAndReturnsDto() throws Exception {
        String userId = UUID.randomUUID().toString();
        String logId = UUID.randomUUID().toString();
        when(systemLogDAO.save(any(SystemLog.class)))
                .thenReturn(savedLog(logId, "ERROR", "PaymentGateway", "timeout", userId));

        SystemLogDTO result = service.log("ERROR", "PaymentGateway", "timeout", userId);

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogDAO).save(captor.capture());
        SystemLog toSave = captor.getValue();
        assertEquals("ERROR", toSave.getLogLevel());
        assertEquals("PaymentGateway", toSave.getSource());
        assertEquals("timeout", toSave.getMessage());
        assertEquals(userId, toSave.getUserId());

        assertEquals(logId, result.getLogId());
        assertEquals("ERROR", result.getLogLevel());
    }

    @Test
    @DisplayName("log allows a null userId — system-originated entries have no acting user")
    void log_allowsNullUserId() throws Exception {
        when(systemLogDAO.save(any(SystemLog.class)))
                .thenReturn(savedLog(UUID.randomUUID().toString(), "INFO", "SchedulerJob", "tick", null));

        SystemLogDTO result = service.log("INFO", "SchedulerJob", "tick", null);

        assertNull(result.getUserId());
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll maps each DAO entity into a DTO while preserving pagination metadata")
    void findAll_mapsDtos() throws Exception {
        SystemLog entity = savedLog(UUID.randomUUID().toString(), "WARNING", "SchedulerJob", "slow", null);
        PageRequest request = CursorPagination.firstPage();
        PageResult<SystemLog> daoResult = CursorPagination.toResult(List.of(entity), request, SystemLog::getCreatedAt);
        when(systemLogDAO.findAll(request)).thenReturn(daoResult);

        PageResult<SystemLogDTO> result = service.findAll(request);

        assertEquals(1, result.getCount());
        assertEquals(entity.getLogId(), result.getItems().get(0).getLogId());
        assertEquals("WARNING", result.getItems().get(0).getLogLevel());
    }

    // ── findByLevel ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findByLevel maps every DAO entity at that level into a DTO")
    void findByLevel_mapsDtos() throws Exception {
        SystemLog entity = savedLog(UUID.randomUUID().toString(), "ERROR", "PaymentGateway", "failed", null);
        when(systemLogDAO.findByLevel("ERROR")).thenReturn(List.of(entity));

        List<SystemLogDTO> result = service.findByLevel("ERROR");

        assertEquals(1, result.size());
        assertEquals("ERROR", result.get(0).getLogLevel());
        verify(systemLogDAO).findByLevel("ERROR");
    }

    @Test
    @DisplayName("findByLevel returns an empty list when the DAO finds nothing")
    void findByLevel_returnsEmptyList_whenNoneFound() throws Exception {
        when(systemLogDAO.findByLevel("DEBUG")).thenReturn(List.of());

        List<SystemLogDTO> result = service.findByLevel("DEBUG");

        assertTrue(result.isEmpty());
    }

    // ── purgeOlderThanDays ────────────────────────────────────────────────

    @Test
    @DisplayName("purgeOlderThanDays delegates to the DAO and returns the deleted row count")
    void purgeOlderThanDays_delegatesToDao() throws Exception {
        when(systemLogDAO.deleteOlderThanDays(60)).thenReturn(12);

        int result = service.purgeOlderThanDays(60);

        assertEquals(12, result);
        verify(systemLogDAO).deleteOlderThanDays(60);
    }
}
