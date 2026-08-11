package hospital.management.backend.dao.support;

import hospital.management.backend.config.db.DBConnection;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Shared base for DAO integration tests: one real Postgres instance, started
 * once via the {@code docker} CLI directly and reused for the whole JVM/test
 * run (starting a fresh container per class would make a 24-DAO test suite
 * take many minutes just in container boot time).
 *
 * The Testcontainers Java library was tried first but its bundled docker-java
 * client hardcodes an initial health-check request to the legacy Docker API
 * version v1.32; this environment's Docker Engine rejects that specific
 * version with a malformed 400 response even though the plain {@code docker}
 * CLI and modern API calls work fine. Shelling out to {@code docker} directly
 * — exactly what the CLI already does successfully — sidesteps that library
 * bug entirely while achieving the same goal: real Postgres, real SQL.
 *
 * {@link DBConnection#configureForTests} points the app's own connection pool
 * at this container, so every DAOImpl under test runs its real, unmodified
 * SQL against a real Postgres — this is what actually proves the SQL is
 * correct (RETURNING clauses, FK/UNIQUE constraints, updated_at triggers,
 * gen_random_uuid() defaults), which a mocked Connection cannot.
 *
 * hospital_schema.sql is loaded once at container startup. Views/routines are
 * deliberately NOT loaded — no DAOImpl queries a view or depends on an index
 * for correctness, only for production performance, so skipping them keeps
 * setup fast.
 *
 * Every table is truncated after each test (CASCADE, RESTART IDENTITY) so
 * tests never see another test's leftover rows, regardless of which DAO(s)
 * a given test class exercises.
 */
public abstract class PostgresIntegrationTestBase {

    private static final String[] ALL_TABLES = {
            "departments", "doctors", "patients", "appointments", "medical_records",
            "referrals", "patient_allergies", "vital_signs", "medications",
            "medical_inventory", "prescriptions", "prescription_items", "lab_orders",
            "lab_results", "doctor_schedules", "patient_feedback", "invoices",
            "users", "roles", "permissions", "user_roles", "role_permissions",
            "audit_log", "user_sessions", "system_logs"
    };

    /**
     * Overridable via -Dtest.postgres.container/-Dtest.postgres.port so independent
     * batches of DAO test authoring (e.g. separate agents working in parallel) can each
     * verify against their own isolated container instead of colliding on the same
     * fixed name/port. The defaults below are what CI and a normal full local run use.
     */
    private static final String CONTAINER_NAME = System.getProperty("test.postgres.container", "hms-dao-it-postgres");
    private static final int    HOST_PORT      = Integer.parseInt(System.getProperty("test.postgres.port", "55432"));
    private static final String DB_NAME        = "hospital_test";
    private static final String DB_USER        = "test";
    private static final String DB_PASSWORD    = "test";

    static {
        removeAnyExistingContainer();
        startContainer();
        Runtime.getRuntime().addShutdownHook(new Thread(PostgresIntegrationTestBase::removeAnyExistingContainer));
        waitUntilReady();
        DBConnection.configureForTests(
                "jdbc:postgresql://localhost:" + HOST_PORT + "/" + DB_NAME, DB_USER, DB_PASSWORD);
        runSchema();
    }

    private static void removeAnyExistingContainer() {
        runDocker("rm", "-f", CONTAINER_NAME);
    }

    private static void startContainer() {
        int exit = runDocker("run", "-d", "--name", CONTAINER_NAME,
                "-p", HOST_PORT + ":5432",
                "-e", "POSTGRES_DB=" + DB_NAME,
                "-e", "POSTGRES_USER=" + DB_USER,
                "-e", "POSTGRES_PASSWORD=" + DB_PASSWORD,
                "postgres:16-alpine");
        if (exit != 0) {
            throw new IllegalStateException("`docker run` failed (exit " + exit + ") — is Docker running?");
        }
    }

    /**
     * The official postgres image starts a temporary server for initdb, stops
     * it, then starts the real one — pg_isready can report ready during that
     * brief in-between window. Requiring an actual "SELECT 1" against the
     * target database (not just pg_isready) to succeed avoids racing that
     * restart.
     */
    private static void waitUntilReady() {
        Instant deadline = Instant.now().plusSeconds(180);
        while (Instant.now().isBefore(deadline)) {
            if (runDocker("exec", CONTAINER_NAME, "psql", "-U", DB_USER, "-d", DB_NAME, "-c", "SELECT 1") == 0) {
                return;
            }
            sleep(Duration.ofMillis(500));
        }
        throw new IllegalStateException("Postgres container '" + CONTAINER_NAME + "' never became ready within 180s");
    }

    private static void sleep(Duration duration) {
        try {
            TimeUnit.MILLISECONDS.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Postgres container", e);
        }
    }

    private static int runDocker(String... args) {
        try {
            String[] command = new String[args.length + 1];
            command[0] = "docker";
            System.arraycopy(args, 0, command, 1, args.length);
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return process.waitFor();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to invoke docker CLI — is it on PATH?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while invoking docker CLI", e);
        }
    }

    private static void runSchema() {
        String schema = readResource("/hospital/management/sql/hospital_schema.sql");
        SQLException lastFailure = null;
        for (int attempt = 1; attempt <= 5; attempt++) {
            try (Connection conn = DBConnection.getConnection();
                 Statement st = conn.createStatement()) {
                st.execute(schema);
                return;
            } catch (SQLException e) {
                lastFailure = e;
                sleep(Duration.ofMillis(500));
            }
        }
        throw new IllegalStateException("Failed to load schema into the test Postgres container", lastFailure);
    }

    private static String readResource(String path) {
        try (InputStream in = PostgresIntegrationTestBase.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Resource not found on classpath: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }

    @AfterEach
    void truncateAllTables() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("TRUNCATE TABLE " + String.join(", ", ALL_TABLES) + " RESTART IDENTITY CASCADE");
        }
    }
}
