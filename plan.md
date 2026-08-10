# Hospital Management System — Implementation Plan

Tracks all remaining gaps identified against the project user stories.
Mark each task `[x]` when done. PostgreSQL code is never removed or modified for MongoDB purposes.

---

## Execution Order (Dependencies)

```
Phase A — Foundation (must go first)
  A1  pom.xml         → add MongoDB driver
  A2  EnvConfig.java  → expose MONGO_URL getters
  A3  mongo/config/MongoConfig.java  → connection singleton

Phase B — MongoDB store layer (needs A3)
  B1  mongo/store/MongoLogStore.java          → audit + system log writes
  B2  mongo/service/PatientNotesMongoService.java  → patient notes (MongoDB-only)
  B3  mongo/benchmark/MongoBenchmarkService.java   → real Mongo benchmarks

Phase C — Algorithm layer (independent of A/B)
  C1  utils/AlgorithmUtils.java               → mergesort + binary search
  C2  service/department/DoctorServiceImpl.java → hook algorithms into live path

Phase D — Benchmark wiring (needs B3 and C1)
  D1  service/analytics/PerformanceBenchmarkService.java → add patient-search + call MongoBenchmark

Phase E — Documentation (after everything compiles and runs)
  E1  docs/PERFORMANCE_REPORT.md  → replace fabricated Mongo numbers, add query plans, add patient-search row
  E2  docs/TESTING_REPORT.md      → add evidence section with captured test output
  E3  docs/test-evidence/         → captured mvn test output files
```

---

## Phase A — Foundation

### A1 — Add MongoDB driver to `pom.xml`
- [x] Add inside `<dependencies>`:
  ```xml
  <dependency>
      <groupId>org.mongodb</groupId>
      <artifactId>mongodb-driver-sync</artifactId>
      <version>5.1.4</version>
  </dependency>
  ```
- [x] Run `mvn clean install -DskipTests` and confirm it compiles

---

### A2 — Expose `MONGO_URL` in `EnvConfig.java`
**File:** `src/main/java/hospital/management/backend/config/EnvConfig.java`

- [ ] Add a new `── MongoDB ──` section at the bottom (before the closing brace):
  ```java
  // ── MongoDB ───────────────────────────────────────────────────────────────
  public static String getMongoUrl() {
      String v = dotenv.get("MONGO_URL", null);
      if (v == null || v.isBlank()) v = dotenv.get("MONGO_URI", null);
      return v != null ? v : "mongodb://localhost:27017";
  }

  public static String getMongoDatabase() {
      return dotenv.get("MONGO_DATABASE", "hospital_nosql");
  }
  ```
- [ ] Verify `.env` file has `MONGO_URL=<your connection string>` (already present per CLAUDE.md)

---

### A3 — Create `MongoConfig.java`
**New file:** `src/main/java/hospital/management/backend/mongo/config/MongoConfig.java`

- [ ] Create package `hospital.management.backend.mongo.config`
- [ ] Implement as a lazy double-checked singleton (same pattern as `RedisConnection`):
  - Uses `MongoClients.create(EnvConfig.getMongoUrl())`
  - Returns `MongoDatabase` via `client.getDatabase(EnvConfig.getMongoDatabase())`
  - On connection failure: logs a warning and returns `null` — callers must null-check and silently skip
  - Exposes `close()` for graceful shutdown
- [ ] Log on success: `"MongoDB connected: hospital_nosql"`
- [ ] Log on failure: `"MongoDB unavailable — NoSQL writes will be skipped"`

```
Rule: every caller of MongoConfig.getDatabase() must null-check the result.
      A null return means MongoDB is down — skip silently, never throw.
```

---

## Phase B — MongoDB Store Layer

All files go under `src/main/java/hospital/management/backend/mongo/`.
**PostgreSQL code (`DualLogBridge`, `ServiceMongoLogger`, `PatientNotesNoSqlService`) is left exactly as-is.**

---

### B1 — Create `MongoLogStore.java`
**New file:** `src/main/java/hospital/management/backend/mongo/store/MongoLogStore.java`

This is the recreated version of the previously deleted `MongoLogStore`. It writes secondary (non-transactional) copies of audit and system logs to MongoDB. PostgreSQL remains the primary.

- [ ] Create package `hospital.management.backend.mongo.store`
- [ ] Collections: `audit_logs`, `system_logs`
- [ ] Implement:
  ```
  writeAudit(AuditLog log)
    → inserts Document with: log_id, user_id, action, table_affected, record_id, created_at
    → null-checks MongoConfig.getDatabase() first
    → swallows all exceptions internally (logs warning only)

  writeSystem(SystemLog log)
    → inserts Document with: log_id, user_id, level, source, message, created_at
    → null-checks MongoConfig.getDatabase() first
    → swallows all exceptions internally

  findSystemLogsByLevel(String level, int limit) → List<Document>
    → used by MongoBenchmarkService (Phase B3)
    → returns empty list if MongoDB unavailable
  ```
- [ ] All methods are `public static` — same style as the deleted class

---

### B2 — Create `PatientNotesMongoService.java`
**New file:** `src/main/java/hospital/management/backend/mongo/service/PatientNotesMongoService.java`

This is the true MongoDB-backed patient notes service. It lives separately from the existing
`PatientNotesNoSqlService` (which stores to PostgreSQL and is left untouched).

- [ ] Create package `hospital.management.backend.mongo.service`
- [ ] Collection: `patient_notes`
- [ ] Document shape matches `PatientNoteDTO` fields:
  ```
  note_id        (String UUID, generated on insert)
  patient_id     (String)
  appointment_id (String, nullable)
  author_user_id (String, nullable)
  author_role    (String)
  note_text      (String)
  source         (String, default "medical_records")
  created_at     (Date)
  ```
- [ ] Implement:
  ```
  saveNote(patientId, appointmentId, authorUserId, authorRole, noteText) → String noteId
    → inserts document, returns generated note_id
    → returns null if MongoDB unavailable

  findByPatientId(String patientId) → List<PatientNoteDTO>
    → filters by patient_id, excludes soft-deleted (deleted_at absent)
    → sorted by created_at DESC
    → returns empty list if unavailable

  findByAppointmentId(String appointmentId) → List<PatientNoteDTO>
    → filters by appointment_id
    → sorted by created_at DESC
    → returns empty list if unavailable

  deleteNote(String noteId) → void
    → soft delete: sets deleted_at = new Date() on the document
  ```
- [ ] Map `Document → PatientNoteDTO` in a private `mapDoc()` helper
- [ ] All public methods null-check `MongoConfig.getDatabase()` and swallow exceptions

---

### B3 — Create `MongoBenchmarkService.java`
**New file:** `src/main/java/hospital/management/backend/mongo/benchmark/MongoBenchmarkService.java`

Runs the MongoDB side of the benchmark. Called from `PerformanceBenchmarkService` in Phase D.

- [ ] Create package `hospital.management.backend.mongo.benchmark`
- [ ] Collection used: `benchmark_temp` (dropped after each benchmark run)
- [ ] Implement these 5 methods (all return `BenchmarkResult` — reuse the inner class from `PerformanceBenchmarkService` or define a simple POJO):
  ```
  benchmarkInsertSingle()   → 100 iterations, inserts one Document per iteration
  benchmarkInsertBatch()    → 1 iteration, inserts 100 Documents with insertMany()
  benchmarkFetchByLevel()   → 50 iterations, find({"level": "INFO"}).limit(20)
  benchmarkKeywordSearch()  → 50 iterations, find($regex on "message" field).limit(20)
  benchmarkFetchRecent()    → 50 iterations, find().sort(created_at DESC).limit(50)
  ```
- [ ] Timing pattern: `System.nanoTime()` before loop, `System.nanoTime()` after; `avgMs = elapsed / iterations / 1_000_000.0`
- [ ] Drop `benchmark_temp` collection at the end of each method
- [ ] Returns a `List<BenchmarkResult> runAll()` convenience method
- [ ] If MongoDB is unavailable: all results return `avgMs = -1` (sentinel for "not run")

---

## Phase C — Algorithm Layer

### C1 — Create `AlgorithmUtils.java`
**New file:** `src/main/java/hospital/management/backend/utils/AlgorithmUtils.java`

- [ ] Create in existing `utils` package alongside `ValidatorUtils.java`
- [ ] Implement **Merge Sort** — generic `<T>`, in-place on `List<T>`, `Comparator<T>`:
  - Time: O(n log n) all cases. Space: O(n) auxiliary. Stable: yes.
  - Top-down recursive split → merge.
  - Javadoc must state complexity and when to use over SQL ORDER BY.
- [ ] Implement **Binary Search** — generic `<T, K extends Comparable<K>>`, sorted `List<T>`, key extractor `Function<T,K>`:
  - Time: O(log n). Space: O(1). Precondition: list sorted by same key.
  - Returns index of match or `-1` if not found.
  - Javadoc must state complexity and precondition.
- [ ] No external dependencies — pure Java only

---

### C2 — Hook algorithms into `DoctorServiceImpl.java`
**File:** `src/main/java/hospital/management/backend/service/department/DoctorServiceImpl.java`

- [ ] In `findByDepartment(String departmentId)`:
  - After building `List<DoctorSummaryDTO> dtos` from DAO results and before calling `CacheService.set(...)`, add:
    ```java
    AlgorithmUtils.mergeSort(dtos,
        Comparator.comparing(DoctorSummaryDTO::getFullName, String.CASE_INSENSITIVE_ORDER));
    ```
  - This ensures every cached department doctor list is alphabetically sorted.
- [ ] Add a new method `findDoctorInCachedDepartment(String departmentId, String targetDoctorId) → Optional<DoctorSummaryDTO>`:
  - Reads from cache (returns `Optional.empty()` if not cached)
  - Copies list, sorts by `doctorId` using `AlgorithmUtils.mergeSort`
  - Calls `AlgorithmUtils.binarySearch(sorted, targetDoctorId, DoctorSummaryDTO::getDoctorId)`
  - Returns `Optional.of(result)` or `Optional.empty()`
  - Javadoc: "Demonstrates O(log n) in-memory search on a cached, sorted list."
- [ ] Add import: `import hospital.management.backend.utils.AlgorithmUtils;`

---

## Phase D — Benchmark Wiring

### D1 — Extend `PerformanceBenchmarkService.java`
**File:** `src/main/java/hospital/management/backend/service/analytics/PerformanceBenchmarkService.java`

- [ ] Add call to `MongoBenchmarkService.runAll()` inside `generateBenchmarkReport()`, appending results after the existing PostgreSQL rows
- [ ] Add `benchmarkPatientNameSearchPostgres()` method:
  - 50 iterations of:
    ```sql
    SELECT patient_id, first_name, last_name
    FROM patients
    WHERE (first_name ILIKE ? OR last_name ILIKE ?) AND deleted_at IS NULL
    LIMIT 20
    ```
    with parameter `%a%`
  - Same timing pattern as existing methods
  - Label: `"Patient Name ILIKE Search — PostgreSQL (50 iterations)"`
- [ ] Add this benchmark to the report output after existing PostgreSQL rows
- [ ] In `buildReportMarkdown()`, prefix row labels with `[PG]` or `[Mongo]` to distinguish in the output table

---

## Phase E — Documentation

### E1 — Update `docs/PERFORMANCE_REPORT.md`

- [ ] **Replace fabricated MongoDB numbers** in Section 7 benchmark matrix with real values from `MongoBenchmarkService.runAll()` — run the benchmark, copy the printed numbers
- [ ] **Add patient name search row** to Section 7 using numbers from `benchmarkPatientNameSearchPostgres()`
- [ ] **Add Section 7.1 — EXPLAIN ANALYZE snapshots**: run each query below against the live database and paste the raw output in a fenced `sql` block:
  ```sql
  -- 1. Patient name search (before and after trigram index)
  EXPLAIN ANALYZE
  SELECT patient_id, first_name, last_name
  FROM patients
  WHERE (first_name ILIKE '%a%' OR last_name ILIKE '%a%') AND deleted_at IS NULL
  LIMIT 20;

  -- 2. Patient fetch by ID
  EXPLAIN ANALYZE
  SELECT * FROM patients WHERE patient_id = '<any-uuid>' AND deleted_at IS NULL;

  -- 3. System log recent fetch
  EXPLAIN ANALYZE
  SELECT * FROM system_logs ORDER BY created_at DESC LIMIT 100;

  -- 4. Audit log by user + time range
  EXPLAIN ANALYZE
  SELECT * FROM audit_log
  WHERE user_id = '<any-uuid>' AND created_at >= NOW() - INTERVAL '7 days'
  ORDER BY created_at DESC LIMIT 50;
  ```
- [ ] Check off `[x] Query plan snapshots included` in the acceptance checklist (currently unchecked)

---

### E2 — Update `docs/TESTING_REPORT.md`

- [ ] Add a new `## 6. Query Result Evidence` section at the bottom
- [ ] Reference `docs/test-evidence/full-test-run.txt` for the full Maven output
- [ ] Paste a representative excerpt: Surefire summary lines for `PatientDAOImplTest`, `ValidatorUtilsTest`, `AlgorithmUtilsTest`
- [ ] Add a subsection `### 6.1 Validation Outcomes` showing a few passing validation test method names and the assertion they verify

---

### E3 — Capture test evidence
**New directory:** `docs/test-evidence/`

- [ ] Run `mvn test 2>&1 | tee docs/test-evidence/full-test-run.txt` and commit the output file
- [ ] After C1 is done, write `AlgorithmUtilsTest.java` under `src/test/` and run it; paste pass output in TESTING_REPORT.md
- [ ] After B2 is done, write `PatientNotesMongoServiceTest.java` under `src/test/` with at least: `saveNote_persistsDocument`, `findByPatientId_returnsMatchingNotes`, `findByPatientId_returnsEmptyWhenMongoUnavailable`

---

## New Files Summary

| File | Package | Purpose |
|------|---------|---------|
| `mongo/config/MongoConfig.java` | `backend.mongo.config` | MongoDB connection singleton |
| `mongo/store/MongoLogStore.java` | `backend.mongo.store` | Writes audit + system logs to MongoDB |
| `mongo/service/PatientNotesMongoService.java` | `backend.mongo.service` | Reads/writes patient notes to MongoDB |
| `mongo/benchmark/MongoBenchmarkService.java` | `backend.mongo.benchmark` | Real MongoDB benchmark methods |
| `utils/AlgorithmUtils.java` | `backend.utils` | Merge sort + binary search with complexity docs |
| `docs/test-evidence/full-test-run.txt` | — | Captured `mvn test` output |

## Modified Files Summary

| File | Change |
|------|--------|
| `pom.xml` | Add `mongodb-driver-sync:5.1.4` dependency |
| `config/EnvConfig.java` | Add `getMongoUrl()` and `getMongoDatabase()` |
| `service/department/DoctorServiceImpl.java` | Add merge sort in `findByDepartment`, add `findDoctorInCachedDepartment` |
| `service/analytics/PerformanceBenchmarkService.java` | Add patient-search benchmark, call `MongoBenchmarkService` |
| `docs/PERFORMANCE_REPORT.md` | Replace fabricated numbers, add query plans, add patient-search row |
| `docs/TESTING_REPORT.md` | Add Section 6 evidence |

## Files Left Untouched (PostgreSQL — do not modify)

| File | Why kept |
|------|---------|
| `service/patient/PatientNotesNoSqlService.java` | PostgreSQL implementation, works correctly |
| `service/log/DualLogBridge.java` | PostgreSQL log routing, works correctly |
| `service/log/ServiceMongoLogger.java` | PostgreSQL logger, works correctly |
| `dao/**` | All DAOs are PostgreSQL, complete and correct |
| `hospital_schema.sql` | Schema is final |
| `hospital_indexes_postgresql.sql` | Indexes are final |

---

## Completion Checklist

- [x] A1 — MongoDB driver in pom.xml
- [x] A2 — EnvConfig MONGO_URL getters
- [x] A3 — MongoConfig.java
- [x] B1 — MongoLogStore.java
- [x] B2 — PatientNotesMongoService.java
- [x] B3 — MongoBenchmarkService.java
- [x] C1 — AlgorithmUtils.java
- [x] C2 — DoctorServiceImpl algorithm hooks
- [x] D1 — PerformanceBenchmarkService extended
- [ ] E1 — PERFORMANCE_REPORT.md updated
- [ ] E2 — TESTING_REPORT.md evidence section
- [ ] E3 — docs/test-evidence/ captured output