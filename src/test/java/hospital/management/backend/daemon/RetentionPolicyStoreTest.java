package hospital.management.backend.daemon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetentionPolicyStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadShouldWorkWithoutRecursion() throws Exception {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        try {
            RetentionPolicy policy = new RetentionPolicy();
            policy.setInactiveUserDays(10);
            policy.setDbLogRetentionDays(20);
            policy.setFileLogMaxSizeMb(30);
            policy.setArchiveRetentionDays(40);
            policy.setCleanupIntervalHours(50);

            RetentionPolicyStore.save(policy);

            Path storePath = tempDir.resolve(".hms").resolve("retention.properties");
            assertTrue(Files.exists(storePath));

            RetentionPolicy loaded = RetentionPolicyStore.load();
            assertEquals(10, loaded.getInactiveUserDays());
            assertEquals(20, loaded.getDbLogRetentionDays());
            assertEquals(30, loaded.getFileLogMaxSizeMb());
            assertEquals(40, loaded.getArchiveRetentionDays());
            assertEquals(50, loaded.getCleanupIntervalHours());
        } finally {
            if (originalHome != null) {
                System.setProperty("user.home", originalHome);
            }
        }
    }
}
