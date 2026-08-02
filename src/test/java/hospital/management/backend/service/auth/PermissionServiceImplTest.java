package hospital.management.backend.service.auth;

import hospital.management.backend.dao.auth.interfaces.PermissionDAO;
import hospital.management.backend.dto.auth.CreatePermissionDTO;
import hospital.management.backend.dto.auth.PermissionDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.user.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Ids used here are fresh random UUIDs rather than fixed literals — findById() reads
 * through a real, JVM-wide, static L1 in-process CacheService with no reset hook exposed
 * to tests, so a fixed id would risk one test's cached DTO leaking into another test's
 * assertions (see RoleServiceImplTest/PatientServiceImplTest for the same rationale).
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private PermissionDAO permissionDAO;

    private PermissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PermissionServiceImpl(permissionDAO);
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create throws IllegalArgumentException when resource is blank")
    void create_throwsIllegalArgumentException_whenResourceBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreatePermissionDTO("  ", "create")));
        verifyNoInteractions(permissionDAO);
    }

    @Test
    @DisplayName("create throws IllegalArgumentException when action is blank")
    void create_throwsIllegalArgumentException_whenActionBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreatePermissionDTO("patients", "  ")));
        verifyNoInteractions(permissionDAO);
    }

    @Test
    @DisplayName("create throws ValidationException when the same resource/action pair already exists")
    void create_throwsValidationException_whenPermissionAlreadyExists() throws Exception {
        when(permissionDAO.findByResourceAndAction("patients", "create"))
                .thenReturn(Optional.of(new Permission("perm-1", "patients", "create", null, null, null)));

        assertThrows(ValidationException.class,
                () -> service.create(new CreatePermissionDTO("patients", "create")));
        verify(permissionDAO, never()).save(any());
    }

    @Test
    @DisplayName("create saves a brand-new permission when the resource/action pair is unique")
    void create_savesPermission_whenUnique() throws Exception {
        when(permissionDAO.findByResourceAndAction("patients", "create")).thenReturn(Optional.empty());
        when(permissionDAO.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        PermissionDTO result = service.create(new CreatePermissionDTO("patients", "create"));

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionDAO).save(captor.capture());
        assertEquals("patients", captor.getValue().getResource());
        assertEquals("create", captor.getValue().getAction());
        assertEquals("patients", result.getResource());
        assertEquals("create", result.getAction());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns a mapped DTO when the DAO finds a matching permission")
    void findById_returnsMappedDto_whenFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(permissionDAO.findById(id))
                .thenReturn(Optional.of(new Permission(id, "patients", "create", null, null, null)));

        PermissionDTO dto = service.findById(id);

        assertEquals(id, dto.getPermissionId());
        assertEquals("patients", dto.getResource());
        assertEquals("create", dto.getAction());
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the DAO finds nothing")
    void findById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(permissionDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll maps every DAO permission to a DTO")
    void findAll_mapsEveryPermission() throws Exception {
        when(permissionDAO.findAll()).thenReturn(List.of(
                new Permission("perm-1", "appointments", "read", null, null, null),
                new Permission("perm-2", "patients", "create", null, null, null)));

        List<PermissionDTO> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals("appointments", result.get(0).getResource());
        assertEquals("patients", result.get(1).getResource());
    }

    @Test
    @DisplayName("findAll returns an empty list when the DAO has no permissions")
    void findAll_returnsEmptyList_whenNoPermissions() throws Exception {
        when(permissionDAO.findAll()).thenReturn(List.of());

        assertTrue(service.findAll().isEmpty());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete delegates to the DAO's soft-delete for the given id")
    void delete_delegatesToSoftDelete() throws Exception {
        String id = UUID.randomUUID().toString();

        service.delete(id);

        verify(permissionDAO).softDelete(id);
    }
}
