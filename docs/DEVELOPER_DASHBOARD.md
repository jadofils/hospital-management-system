# Developer Dashboard

The Developer Dashboard (`PageRoute.DEVELOPER_DASHBOARD`, admin-only) is an internal operations page that surfaces real-time infrastructure health, live performance benchmarks, and system metadata in one place without requiring external tooling.

## Access

Navigate to the page via the **Admin** sidebar section → **Developer Dashboard**, or directly after login as any `ADMIN`-role account. The route is protected by `PermissionGate` — non-admin roles see the button hidden and cannot navigate there.

---

## Page Layout

### Connection Status Cards

Four stat cards across the top show the health of each infrastructure component at page load time:

| Card | What it checks | Green | Yellow/Red |
|------|---------------|-------|-----------|
| PostgreSQL | `DBConnection.getConnection()` success | Connected | Error + message |
| MongoDB | `MongoConfig.getDatabase() != null` | Connected | Unavailable |
| Redis Cache | `RedisConnection.isHealthy()` | Connected (L2 active) | Unavailable (L1 still active) |
| Benchmarks Run | Session counter | — | Increments each run |

Connection checks run synchronously on `initialize()` so status is visible immediately. PostgreSQL and MongoDB errors display a truncated error message (≤ 40 chars) in the trend sub-label for quick diagnosis.

### Benchmark Results Table

Columns: **Operation**, **Avg (ms)**, **Throughput (ops/s)**, **Status** (OK / Skipped).

- `Avg (ms) < 0` means the benchmark was skipped (database unavailable) — shown as "N/A" / "Skipped".
- The table uses `CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN` so it fills its panel width.
- Populated only after "Run Benchmark" completes.

### Latency Charts

Two side-by-side `BarChart` panels:

- **PostgreSQL — Avg Latency (ms)**: one bar per `[PG]` benchmark.
- **MongoDB — Avg Latency (ms)**: one bar per `[Mongo]` benchmark.

Operation labels are shortened to ≤ 22 characters for chart readability. Charts are cleared and repopulated on each benchmark run.

### System Information

A `GridPane` showing 6 read-only labels populated at initialize time:

| Row | Value |
|-----|-------|
| Java Runtime | `java.version` + `java.vm.name` system properties |
| PostgreSQL Driver | JDBC 42.7.4 + HikariCP 7.0.2 |
| MongoDB Driver | mongodb-driver-sync 5.1.4 · DB: hospital_nosql (or offline message) |
| Redis Client | Jedis 5.2.0 · connection health |
| Cache Strategy | L1 (in-process LRU, 500 entries, 5-min idle TTL) + L2 (Redis, delete-before-write) |
| Algorithms | MergeSort O(n log n) + BinarySearch O(log n) via AlgorithmUtils |

---

## Running a Benchmark

1. Click **Run Benchmark**. The button shows a spinner while running.
2. All navigation buttons are disabled during the run to prevent scene switches mid-benchmark.
3. Benchmarks execute on a background thread via `AsyncJobRunner` — the JavaFX UI thread stays responsive.
4. On completion, the results table and charts populate, the session counter increments, and the **Download Report** button becomes enabled.
5. A benchmark run takes approximately 20–30 seconds on a typical development machine.

### What Gets Benchmarked

PostgreSQL benchmarks (all use real DB connections via HikariCP):

| Label | Description |
|-------|-------------|
| `[PG] Insert Single Record (100 iterations)` | 100 sequential inserts into `system_logs` · avg per insert |
| `[PG] Insert Batch (100 records)` | Single batch of 100 rows in one transaction |
| `[PG] Fetch by User + Time Range (50 iterations)` | `audit_log` query with UUID user filter + 7-day window |
| `[PG] Fetch Recent 100 Records (50 iterations)` | `system_logs ORDER BY created_at DESC LIMIT 100` |
| `[PG] Update Field (50 records)` | Insert + update 50 rows, measures update path only |
| `[PG] Patient Name ILIKE Search (50 iterations)` | `patients WHERE first_name ILIKE '%a%'` — exercises trigram index |

MongoDB benchmarks (via `MongoBenchmarkService`, degrade to `avgMs = -1` if MongoDB unavailable):

| Label | Description |
|-------|-------------|
| `[Mongo] Insert Single Document (100 iterations)` | 100 sequential inserts into `benchmark_temp` |
| `[Mongo] Insert Batch (100 documents)` | `insertMany` of 100 documents |
| `[Mongo] Fetch by Log Level (50 iterations)` | Field-equality query on `log_level` |
| `[Mongo] Keyword Search (50 iterations)` | Regex search on `message` field |
| `[Mongo] Fetch Recent 100 Documents (50 iterations)` | Sort descending by `created_at`, limit 100 |

The `benchmark_temp` collection is dropped after each MongoDB benchmark run so it leaves no permanent data.

---

## Downloading the Report

After a benchmark run completes, click **Download Report**. A file picker opens pre-populated with `performance_benchmark_report.md`. The report is a Markdown table with all operations, avg latency, and throughput. If the system supports `Desktop.open()`, the file opens automatically after saving.

The report is also stored as a temp file for the duration of the session (path printed to log). Multiple runs overwrite the temp file but the downloaded copy is independent.

---

## Architecture

```
DeveloperDashboardController
  ├── initialize()
  │     ├── refreshStatusCards()   — PG/Mongo/Redis health checks
  │     ├── loadSystemInfo()       — static version strings
  │     └── setupTable()           — column factories
  │
  ├── runBenchmark()               — via AsyncJobRunner background thread
  │     ├── PerformanceBenchmarkService.runBenchmarks()
  │     │     ├── [PG] benchmarks (6 operations, real JDBC)
  │     │     └── MongoBenchmarkService.runAll() (5 operations, real Mongo)
  │     ├── PerformanceBenchmarkService.generateBenchmarkReport()
  │     └── → UI update on FX thread: table, charts, counters
  │
  └── downloadReport()             — FileChooser → Files.copy → Desktop.open
```

### Key Classes

| Class | Package | Role |
|-------|---------|------|
| `DeveloperDashboardController` | `pages/developer/` | JavaFX controller |
| `PerformanceBenchmarkService` | `backend/service/analytics/` | PostgreSQL benchmarks + report generation |
| `MongoBenchmarkService` | `backend/mongo/benchmark/` | MongoDB benchmarks |
| `AlgorithmUtils` | `backend/utils/` | MergeSort + BinarySearch (shown in System Info) |
| `AsyncJobRunner` | `backend/utils/pipes/` | Background execution, FX-thread callbacks |
| `MongoConfig` | `backend/mongo/config/` | MongoDB singleton, null-on-failure |
| `RedisConnection` | `backend/config/cache/` | Redis health check |
| `DBConnection` | `backend/config/db/` | HikariCP PostgreSQL pool |

---

## Notes for Developers

- **Benchmark data is temporary.** Benchmark inserts are deleted immediately after measurement. No permanent test data is written.
- **MongoDB optional.** If `MONGO_URL` is not set or MongoDB is down, all `[Mongo]` rows show "N/A / Skipped" and the chart has no bars. The rest of the dashboard still works.
- **Redis optional.** Redis unavailability only affects the status card label; L1 cache remains active.
- **Session counter.** `sessionBenchmarkRuns` is a `static int` — it counts runs across page navigations within one JVM session and resets on app restart.
- **Report generation.** `generateBenchmarkReport()` calls `runBenchmarks()` internally, so clicking "Run Benchmark" in the controller actually runs benchmarks twice — once for the live table/chart display and once inside `generateBenchmarkReport()`. This is intentional: the report captures a fresh independent run for file accuracy.