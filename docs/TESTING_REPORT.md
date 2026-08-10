# Testing Report — Hospital Management System

**Date:** 2026-08-02
**Scope:** JUnit 5 test suite covering the entire `backend` layer (utils, config, enums, mappers, DAOs, Services, event bus), plus the CI pipeline and branch-protection gate that enforce it going forward.

---

## 1. Summary

| Metric | Value |
|---|---|
| Total tests | **744** |
| Passing | **744 (100%)** |
| Test source files | 60 |
| Frameworks | JUnit 5, Mockito 5.22.0 |
| Real-database tests | 301 (via a throwaway Postgres container per run) |
| Mocked-dependency tests | 256 |
| Pure/no-dependency tests | 187 |
| CI | GitHub Actions, runs on every push/PR to `master` |
| Branch protection | `master` requires the `Build & Test` check to pass before merge |

Before this effort, the project had **zero tests** (JUnit 5 was configured in `pom.xml` but unused). Every number above is new.

---

## 2. What was tested, and why this grouping

Testing proceeded outward from the safest, cheapest-to-verify code to the riskiest:

### 2.1 Pure/functional classes — 176 tests
No mocks, no database, no Spring-like context — plain input/output verification.

| Area | Class(es) | Tests |
|---|---|---|
| Validation | `ValidatorUtils` | 40 |
| Sanitization | `SanitizeUtils` | 14 |
| Cursor pagination | `CursorPagination` | 10 |
| Password hashing | `PasswordConfig` | 7 |
| Encryption | `EncryptionConfig` | 7 |
| JWT | `JwtConfig` | 5 |
| Enums | `RoleName`, `NotificationType`, `PageRoute` | 9 + 7 + 56 |
| Mappers | `PatientMapper`, `DoctorMapper` | 7 + 8 |
| Event bus | `EventBus` (in-process pub/sub) | 6 |

**Why first:** these classes have no side effects, so a failing test unambiguously means the logic is wrong — no ambiguity from a mock being configured incorrectly or a database being in the wrong state. They also caught real authoring mistakes early that would otherwise have propagated: e.g. an incorrect assumption that `PasswordConfig.hash()` produces `$2b$`-prefixed bcrypt hashes by default (it produces `$2a$`), corrected by constructing a real `$2b$` fixture via `BCrypt.with(BCrypt.Version.VERSION_2B)` and re-verifying against `PasswordConfig.verify()`.

### 2.2 Service layer — 256 tests (Mockito-mocked DAOs)
Every `*ServiceImpl` class, tested with its DAO dependencies mocked via `@Mock`/`@ExtendWith(MockitoExtension.class)`. This isolates business-rule logic (validation, cache invalidation ordering, event publishing, transaction composition) from persistence concerns.

| Domain | Services covered | Tests |
|---|---|---|
| auth | Auth, Permission, Role, User | 45 |
| clinical | Appointment, MedicalRecord | 27 |
| department | Department, Doctor, DoctorSchedule, Referral | 50 |
| finance | Invoice | 16 |
| lab | Lab | 19 |
| log | Audit, SystemLog | 17 |
| patient | Allergy, Feedback, Patient, VitalSign | 45 |
| pharmacy | Pharmacy, Prescription | 37 |

Two non-obvious infrastructure details every Service test had to account for, since both are real, static, JVM-wide singletons with no reset hook:
- **`CacheService`'s in-process L1 cache** — every test that exercises a cache-backed `findById` uses a fresh random `UUID` per test, never a fixed literal, to avoid one test's cached value leaking into another's assertions within the same JVM.
- **`TransactionManager.executeInTransaction(...)`** — the only place a Service opens a real JDBC `Connection`. Any Service method that uses it (e.g. `AuthServiceImpl.login/changePassword`) is tested with `Mockito.mockStatic(TransactionManager.class)`, intercepting the static call and invoking the work lambda directly against a mocked `Connection` — so these tests never touch a real database, yet still exercise the exact transactional-composition code path.

`CacheService`/`RedisConnection` themselves are deliberately left un-mocked in every Service test: both catch connection failures internally and log a warning rather than throwing (confirmed by reading the source), so an unreachable Redis in CI is a safe no-op, not a test failure.

### 2.3 DAO layer — 301 tests (real Postgres, via integration tests)
Every `*DAOImpl` class — the raw JDBC layer — tested against a **real PostgreSQL database**, not mocked `Connection`/`PreparedStatement`/`ResultSet` objects. This is the only way to actually prove the SQL is correct: `RETURNING` clauses, `CHECK`/`UNIQUE`/foreign-key constraints, `updated_at` triggers, and `gen_random_uuid()` defaults can't be verified against a mock — mocking JDBC internals only proves the DAO calls the mock the way the code says to, not that the SQL works.

| Domain | DAOs covered | Tests |
|---|---|---|
| auth | Permission, Role, RolePermission, User, UserRole, UserSession | 67 |
| clinical | Appointment, MedicalRecord | 25 |
| department | Department, Doctor, DoctorSchedule, Referral | 47 |
| finance | Invoice | 18 |
| lab | LabOrder, LabResult | 31 |
| log | AuditLog, SystemLog | 19 |
| patient | Patient, PatientAllergy, PatientFeedback, VitalSign | 48 |
| pharmacy | MedicalInventory, Medication, Prescription, PrescriptionItem | 46 |

**How this was built:**
1. **Testcontainers was tried first** (the standard Java library for this) but its bundled `docker-java` HTTP client hard-codes an initial health-check request to the legacy Docker Engine API version `v1.32`. This machine's Docker Engine (29.6.2) rejects that specific old version with a malformed `400 Bad Request` — a real environment incompatibility, confirmed by testing multiple Testcontainers versions and explicit `DOCKER_API_VERSION` overrides, none of which worked, while the plain `docker` CLI worked perfectly throughout.
2. **Replaced with a direct `docker` CLI wrapper** (`PostgresIntegrationTestBase`): starts one `postgres:16-alpine` container via `ProcessBuilder`, waits for real readiness (`psql ... SELECT 1`, not just `pg_isready`, since the official Postgres image restarts once internally after `initdb` and `pg_isready` can report ready mid-restart), loads `hospital_schema.sql`, and registers a JVM shutdown hook to remove the container on exit.
3. **`DBConnection` was refactored** from an eager `static { }` block (which read `.env` and opened a real connection pool at class-load time) to a lazily-initialized singleton with a test-only `configureForTests(url, user, password)` hook, so integration tests can redirect the app's own unmodified connection pool at the throwaway container instead of `.env`'s configured database — without changing any DAO code.
4. **Isolation between tests:** all 25 tables are `TRUNCATE`d (`RESTART IDENTITY CASCADE`) after every single test, so no test ever sees another's leftover rows — each test inserts whatever parent/FK fixture rows it needs itself.
5. **Parallel authoring, sequential verification:** the 24 remaining DAOs (Patient was written first as the end-to-end proof of concept) and their paired Services were written by 7 parallel agents, one per domain, each against its own isolated container (distinct Docker container name + host port) to avoid clashing with the others mid-write. All work was then collected into one branch and re-verified together as a single, sequential 744-test run before merging — the parallel containers were a write-time convenience, not part of the shipped test suite.

---

## 3. Real bugs and design gaps found through testing

Testing against a real database, not mocks, surfaced genuine discrepancies between what the Service layer assumes and what the schema actually enforces — these were **documented in the tests, not silently patched**, since fixing them is a product decision, not a testing one:

| Finding | Detail |
|---|---|
| **`patients.email` has no `UNIQUE` constraint** | Unlike `doctors.email` and `users.email`, which do. `PatientServiceImpl.create()` only prevents duplicates via a check-then-insert (`findByEmail` then `save`), which is race-prone under concurrent requests — two simultaneous creates could both pass the check before either inserts. Covered by `PatientDAOImplTest.save_allowsDuplicateEmail_atDaoLevel`, which documents the DAO's actual (permissive) behavior. |
| **`DepartmentServiceImpl.findAll()` caches under a fixed key** | `"department:list"`, with no parameters — unlike every other list-cache key in `CacheKey`, which is parameterized (e.g. by page/filter). Not necessarily wrong (department lists are small and global), but inconsistent with the rest of the caching scheme and worth a second look. |
| **`doctor_schedules`/`referrals` CHECK constraints have no Java-side pre-validation** | `chk_schedule_time_order` (end > start), `chk_referral_not_self`, and both tables' `status` value lists are enforced purely by Postgres — a caller only finds out a value is invalid via a `DatabaseException` from the DB, not an earlier, friendlier validation error from the Service layer. |
| **`medical_records.appointment_id` is `UNIQUE NOT NULL`** | Correctly enforces a 1:0..1 appointment–record relationship at the DB level — confirmed as *working as intended*, not a gap, but worth noting since it's the kind of constraint easy to silently break in a future migration. |
| **`AuditLogDAO`/`SystemLogDAO`'s raw `Connection`-overloaded `save(log, conn)`** | Propagates the underlying `SQLException` unwrapped (no try/catch), unlike the plain `save(log)` overload, which wraps it in `DatabaseException`. Relevant for any future caller composing it into a larger transaction — the exception contract differs between the two overloads. |

No `patients.email`-style gap was found in `audit_log`/`system_logs` (both are append-only, no business-key constraints) or in the `clinical` domain (`AppointmentServiceImpl`/`MedicalRecordServiceImpl` validation matches what the schema enforces).

---

## 4. CI and branch protection

- **`.github/workflows/ci.yml`** — runs the full suite (`./mvnw test`) on every push/PR to `master`, on `ubuntu-latest`, Temurin JDK 25. Generates a dummy `.env` inline (real `.env` is gitignored) — only needs to satisfy format checks like `ENCRYPTION_KEY`'s 32-character minimum, never used to open a real connection. DAO integration tests start their own Postgres via the `docker` CLI, which GitHub-hosted runners support out of the box. Tests run under `xvfb-run` because `EventBusTest` initializes the real JavaFX toolkit (`Platform.startup`) to test `EventBus`'s `Platform.runLater` dispatch path, which needs a display — `ubuntu-latest` is headless.
- **Branch protection on `master`** — requires the `Build & Test` check to pass before any PR can merge (`required_status_checks`, `strict: true`), `enforce_admins: true` (no bypass, including for repo admins), and blocks force-pushes/branch deletion.

---

## 5. What this suite does *not* cover (honest scope note)

- **UI/JavaFX controller layer** (`pages/**`) — no tests. These require either TestFX or manual interaction; out of scope for this pass.
- **`backend/daemon/`** (background schedulers, if any) and **email/Cloudinary integrations** — not exercised.
- **Concurrency/load testing** — the `patients.email` race condition above is inferred from reading the code (check-then-insert with no DB backstop), not reproduced under actual concurrent load.
- **End-to-end flows** spanning multiple Services in one transaction (e.g. "book appointment → issue prescription → generate invoice") are only tested at the single-Service level, not as a full workflow.
