package hospital.management.backend.service.analytics;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestSuiteServiceTest {

    private final TestSuiteService service = new TestSuiteService();

    @Test
    void locateProjectRootFindsPom() {
        String root = service.locateProjectRoot();
        assertTrue(Files.isRegularFile(Path.of(root, "pom.xml")),
            "project root should contain pom.xml, got: " + root);
    }

    @Test
    void parseSurefireReportsOnMissingDirReturnsEmpty() {
        assertTrue(service.parseSurefireReports("target/definitely-not-a-dir-xyz").isEmpty());
    }

    @Test
    void benchmarkAlgorithmsReturnsExpectedRows() {
        List<TestSuiteService.AlgorithmBenchmark> results = service.benchmarkAlgorithms();
        assertEquals(4, results.size(), "sort + search + SHA-256 + BCrypt");
        for (TestSuiteService.AlgorithmBenchmark r : results) {
            assertFalse(r.algorithm().isBlank(), "algorithm name");
            assertFalse(r.complexity().isBlank(), "complexity");
            assertTrue(r.avgMillis() >= 0, r.algorithm() + " avgMillis");
            assertTrue(r.opsPerSec() >= 0, r.algorithm() + " opsPerSec");
        }
    }
}
