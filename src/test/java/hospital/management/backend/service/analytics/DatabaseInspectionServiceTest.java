package hospital.management.backend.service.analytics;

import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test for the Developer Dashboard's regenerate actions.
 *
 * <p>Regression guard: after the SQL-script consolidation the old
 * {@code hospital_objects.sql} / {@code hospital_indexes_postgresql.sql} no longer
 * exist — every regenerate action must extract its statements from
 * {@code hospital_schema.sql} instead. These tests would fail loudly (file not
 * found on classpath) if that wiring ever regressed, and they also prove the
 * replay is idempotent over an already-created schema.
 */
class DatabaseInspectionServiceTest extends PostgresIntegrationTestBase {

    private final DatabaseInspectionService service = new DatabaseInspectionService();

    @Test
    @DisplayName("regenerateIndexes replays CREATE INDEX statements idempotently over an existing schema")
    void regenerateIndexesIsIdempotent() throws Exception {
        int before = service.listIndexes().size();
        assertTrue(before > 0, "schema load should have created indexes");

        service.regenerateIndexes();

        assertEquals(before, service.listIndexes().size(),
                "regenerating over existing indexes must be a no-op, not a duplicate/conflict");
    }

    @Test
    @DisplayName("regenerateIndexes restores a dropped index")
    void regenerateIndexesRestoresDroppedIndex() throws Exception {
        Optional<DatabaseInspectionService.DbIndex> target = service.listIndexes().stream()
                .filter(i -> i.indexName().equals("idx_patients_name"))
                .findFirst();
        assertTrue(target.isPresent(), "idx_patients_name should exist from schema load");

        service.dropIndex("idx_patients_name");
        assertTrue(service.listIndexes().stream()
                        .noneMatch(i -> i.indexName().equals("idx_patients_name")),
                "index should be gone after the drop");

        service.regenerateIndexes();

        assertTrue(service.listIndexes().stream()
                        .anyMatch(i -> i.indexName().equals("idx_patients_name")),
                "regenerate should recreate the dropped index");
    }

    @Test
    @DisplayName("regenerateViews is a safe no-op when the schema defines no views")
    void regenerateViewsWithNoViews() throws Exception {
        service.regenerateViews();

        assertEquals(0, service.listViews().size(),
                "no views are defined anywhere in the consolidated schema");
    }

    @Test
    @DisplayName("regenerateRoutines replays the set_updated_at trigger function idempotently")
    void regenerateRoutinesRecreatesTriggerFunction() throws Exception {
        assertTrue(service.listRoutines().stream()
                        .anyMatch(r -> r.routineName().equals("set_updated_at")),
                "schema load should have created the set_updated_at function");

        service.regenerateRoutines();

        assertTrue(service.listRoutines().stream()
                        .anyMatch(r -> r.routineName().equals("set_updated_at")),
                "set_updated_at must still exist after regenerating routines");
    }
}
