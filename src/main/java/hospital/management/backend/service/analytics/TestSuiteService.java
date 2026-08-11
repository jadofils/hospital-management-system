package hospital.management.backend.service.analytics;

import at.favre.lib.crypto.bcrypt.BCrypt;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.utils.AlgorithmUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Drives the real test suite from the Developer dashboard.
 *
 * <p>Two complementary halves:
 * <ol>
 *   <li><b>Suite execution</b> — spawns {@code mvn -B test} as a subprocess and
 *       streams its console output back to the UI, then parses the Surefire
 *       {@code target/surefire-reports/TEST-*.xml} files into per-class results
 *       (JUnit 5 against PostgreSQL DAOs, Mockito-mocked services, etc.).</li>
 *   <li><b>Algorithm &amp; hashing benchmarks</b> — runs fast, in-process
 *       micro-benchmarks of the DSA and hashing primitives the app actually uses
 *       ({@link AlgorithmUtils} merge sort / binary search, SHA-256, BCrypt cost
 *       12) so "speed" can be shown without a Maven round-trip.</li>
 * </ol>
 */
public class TestSuiteService {

    private static final AppLogger logger = AppLogger.getLogger(TestSuiteService.class);

    /** One Surefire report = one test class. */
    public record ClassResult(String className, int tests, int failures, int errors, int skipped, double timeSeconds) {}

    /** Aggregated result of a full {@code mvn test} run. */
    public record SuiteSummary(List<ClassResult> classes, int tests, int failures, int errors,
                               int skipped, double durationSeconds, boolean success,
                               boolean cancelled, String projectDir) {}

    /** One row of the in-process algorithm benchmark table. */
    public record AlgorithmBenchmark(String algorithm, String complexity, double avgMillis,
                                     double opsPerSec, String note) {}

    // ── Project root discovery ───────────────────────────────────────────────

    /**
     * Finds the Maven project root (the directory containing {@code pom.xml}).
     * Searches upward from the working directory so the dashboard works whether
     * the app was launched from the repo root or a nested module directory.
     */
    public String locateProjectRoot() {
        Path dir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (dir != null) {
            if (Files.isRegularFile(dir.resolve("pom.xml"))) return dir.toString();
            dir = dir.getParent();
        }
        return dir == null
            ? Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().toString()
            : dir.toString();
    }

    // ── Suite execution ──────────────────────────────────────────────────────

    /**
     * Runs {@code mvn -B test} in {@code workingDir}, streaming each output line
     * to {@code lineSink} as it is produced. After the process exits, Surefire
     * reports are parsed into a {@link SuiteSummary}.
     *
     * @param lineSink   receives raw console lines (called from the Maven thread —
     *                   wrap in {@code Platform.runLater} if it touches the UI)
     * @param cancelled  when set {@code true}, the process is destroyed and the
     *                   run aborts at the next output line
     */
    public SuiteSummary runMavenTests(String workingDir, Consumer<String> lineSink, AtomicBoolean cancelled)
            throws Exception {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        List<String> command = new ArrayList<>();
        if (windows) {
            command.add("cmd");
            command.add("/c");
        }
        command.add("mvn");
        command.add("-B");
        command.add("-q");
        command.add("test");

        lineSink.accept("$ " + String.join(" ", command) + "   (working dir: " + workingDir + ")");
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(workingDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (cancelled.get()) {
                    process.destroyForcibly();
                    lineSink.accept("\n[test run cancelled by user]");
                    break;
                }
                lineSink.accept(line);
            }
        }

        int exit;
        for (;;) {
            try {
                exit = process.waitFor();
                break;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        lineSink.accept("[maven exit code: " + exit + "]");

        List<ClassResult> classes = parseSurefireReports(workingDir);
        SuiteSummary summary = toSummary(classes, exit, cancelled.get(), workingDir);
        if (cancelled.get()) {
            lineSink.accept("[run cancelled — showing partial results if any]");
        } else {
            lineSink.accept("[parsed " + classes.size() + " test class(es) from target/surefire-reports]");
        }
        return summary;
    }

    // ── Surefire report parsing ──────────────────────────────────────────────

    /** Reads {@code target/surefire-reports/TEST-*.xml} into per-class results. */
    public List<ClassResult> parseSurefireReports(String workingDir) {
        Path reportDir = Paths.get(workingDir, "target", "surefire-reports");
        if (!Files.isDirectory(reportDir)) return List.of();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        List<ClassResult> out = new ArrayList<>();
        try (var stream = Files.list(reportDir)) {
            stream.filter(p -> p.getFileName().toString().startsWith("TEST-")
                    && p.toString().endsWith(".xml"))
                .sorted()
                .forEach(p -> {
                    try {
                        var doc = factory.newDocumentBuilder().parse(p.toFile());
                        var root = doc.getDocumentElement();
                        out.add(new ClassResult(
                            root.getAttribute("name"),
                            parseInt(root.getAttribute("tests")),
                            parseInt(root.getAttribute("failures")),
                            parseInt(root.getAttribute("errors")),
                            parseInt(root.getAttribute("skipped")),
                            parseDouble(root.getAttribute("time"))));
                    } catch (Exception e) {
                        logger.warn("Could not parse surefire report " + p + ": " + e.getMessage());
                    }
                });
        } catch (Exception e) {
            logger.warn("Could not list surefire reports: " + e.getMessage());
        }
        return out;
    }

    // ── Summary aggregation ──────────────────────────────────────────────────

    private SuiteSummary toSummary(List<ClassResult> classes, int mavenExit, boolean cancelled, String projectDir) {
        int tests = 0, failures = 0, errors = 0, skipped = 0;
        double time = 0;
        for (ClassResult r : classes) {
            tests += r.tests();
            failures += r.failures();
            errors += r.errors();
            skipped += r.skipped();
            time += r.timeSeconds();
        }
        boolean success = !cancelled && mavenExit == 0 && failures == 0 && errors == 0;
        return new SuiteSummary(List.copyOf(classes), tests, failures, errors, skipped, time, success, cancelled, projectDir);
    }

    // ── In-process algorithm & hashing benchmarks ────────────────────────────

    /**
     * Runs the micro-benchmarks in the current JVM. These are quick (< a few
     * seconds) and touch no databases, so they run fine on the background pool.
     */
    public List<AlgorithmBenchmark> benchmarkAlgorithms() {
        return List.of(
            benchmarkSorting(),
            benchmarkSearch(),
            benchmarkSha256(),
            benchmarkBcrypt());
    }

    private AlgorithmBenchmark benchmarkSorting() {
        int n = 200_000;
        List<Integer> base = new Random(42L).ints(n).boxed().collect(Collectors.toList());

        for (int i = 0; i < 2; i++) {
            List<Integer> l = new ArrayList<>(base);
            AlgorithmUtils.mergeSort(l, Integer::compareTo);
        }
        long mergeTotal = 0;
        for (int i = 0; i < 5; i++) {
            List<Integer> l = new ArrayList<>(base);
            long s = System.nanoTime();
            AlgorithmUtils.mergeSort(l, Integer::compareTo);
            mergeTotal += System.nanoTime() - s;
        }
        long mergeAvg = mergeTotal / 5;

        long collTotal = 0;
        for (int i = 0; i < 5; i++) {
            List<Integer> l = new ArrayList<>(base);
            long s = System.nanoTime();
            Collections.sort(l);
            collTotal += System.nanoTime() - s;
        }
        long collAvg = collTotal / 5;

        return new AlgorithmBenchmark(
            "MergeSort (custom, stable)",
            "O(n log n)",
            mergeAvg / 1_000_000.0,
            n * 1_000_000_000.0 / mergeAvg,
            "vs Collections.sort " + fmtMillis(collAvg) + " on " + n + " integers");
    }

    private AlgorithmBenchmark benchmarkSearch() {
        int n = 1_000_000;
        int lookups = 10_000;
        Random rnd = new Random(7L);
        List<Integer> sorted = rnd.ints(n).sorted().boxed().collect(Collectors.toList());
        List<Integer> keys = rnd.ints(lookups).boxed().collect(Collectors.toList());

        long binTotal = 0;
        int foundBin = 0;
        for (int r = 0; r < 3; r++) {
            long s = System.nanoTime();
            for (Integer k : keys) {
                if (AlgorithmUtils.binarySearch(sorted, k, Integer::intValue) >= 0) foundBin++;
            }
            binTotal += System.nanoTime() - s;
        }
        long binAvg = binTotal / 3;

        long linTotal = 0;
        int foundLin = 0;
        for (int r = 0; r < 3; r++) {
            long s = System.nanoTime();
            for (Integer k : keys) {
                if (sorted.contains(k)) foundLin++;
            }
            linTotal += System.nanoTime() - s;
        }
        long linAvg = linTotal / 3;

        return new AlgorithmBenchmark(
            "BinarySearch (custom, O(log n))",
            "O(log n)",
            binAvg / 1_000_000.0,
            lookups * 1_000_000_000.0 / binAvg,
            "vs linear .contains " + fmtMillis(linAvg) + " for " + lookups + " lookups on "
                + n + " sorted ints (found " + foundBin + "/" + foundLin + ")");
    }

    private AlgorithmBenchmark benchmarkSha256() {
        byte[] data = new byte[1024 * 1024];
        new Random(3L).nextBytes(data);

        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            return new AlgorithmBenchmark(
                "SHA-256 (MessageDigest)", "O(n)", 0, 0,
                "unavailable: " + e.getMessage());
        }

        long total = 0;
        for (int i = 0; i < 5; i++) {
            long s = System.nanoTime();
            sha.digest(data);
            total += System.nanoTime() - s;
        }
        long avg = total / 5;
        double miBPerSec = data.length / (1024.0 * 1024.0) / (avg / 1_000_000_000.0);

        return new AlgorithmBenchmark(
            "SHA-256 (MessageDigest)",
            "O(n)",
            avg / 1_000_000.0,
            miBPerSec,
            "1 MiB random input · " + String.format(Locale.ROOT, "%.1f MiB/s", miBPerSec));
    }

    private AlgorithmBenchmark benchmarkBcrypt() {
        int cost = 12;
        char[] password = "Password@12".toCharArray();

        String hash = null;
        long total = 0;
        for (int i = 0; i < 3; i++) {
            long s = System.nanoTime();
            hash = BCrypt.withDefaults().hashToString(cost, password);
            total += System.nanoTime() - s;
        }
        long avg = total / 3;

        String prefix = hash == null ? "?" : hash.substring(0, Math.min(20, hash.length()));
        return new AlgorithmBenchmark(
            "BCrypt (cost " + cost + ")",
            "2^cost ≈ 4096 rounds",
            avg / 1_000_000.0,
            1_000_000_000.0 / avg,
            "used by PasswordConfig · hash prefix: " + prefix + "…");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String fmtMillis(long nanos) {
        return String.format(Locale.ROOT, "%.1fms", nanos / 1_000_000.0);
    }

    private static int parseInt(String s) {
        try {
            return s == null || s.isEmpty() ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDouble(String s) {
        try {
            return s == null || s.isEmpty() ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
