# Bug Fixing Log

Tracks defects found in the `feature/nosql-logs-rbac-cache` working-tree changes (code review, 2026-08-06) plus
runtime issues hit while running the app. Status legend: `Open` / `In Progress` / `Fixed` / `Won't Fix (explain)`.

---

## 0. Runtime crashes (handling first)

### 0.1 — `NoClassDefFoundError: hospital/management/backend/daemon/DatabaseCleanupDaemon` on `mvn clean javafx:run`
- **Status:** Investigated — no code defect found. Monitoring.
- **Symptom:** App fails at `Main.start()` with `ClassNotFoundException` for a class that clearly exists on disk and in source control.
- **Root cause analysis:**
  - `DatabaseCleanupDaemon.java` compiles cleanly — verified with a fresh `mvn clean compile` (exit 0), and the resulting `.class` file is present and correct in `target/classes/.../daemon/`.
  - The plugin (`javafx-maven-plugin:run`) declares `executePhase=process-classes` in its own metadata, so it always forks the lifecycle through `compile` before launching — it is not possible for `javafx:run` to launch against a classpath that skipped compilation.
  - The project directory is a genuine local folder (not OneDrive Known-Folder-Move — confirmed via `fsutil reparsepoint query`, not a reparse point), so cloud-sync placeholder files are ruled out.
  - Windows Defender real-time protection is **enabled** on this machine. The most likely explanation is a transient AV file-lock/scan race: `mvn clean` deletes and recreates hundreds of `.class` files in one burst, and Defender's real-time scanner can momentarily hold a lock on a just-written file before the JVM's classloader opens it a moment later — this reproduces as exactly this shape of error (one specific class missing, everything else fine) and typically does not recur on an immediate retry.
- **Action taken:** Re-ran `mvn clean compile` — succeeded, class present. No source change needed.
- **Recommended mitigation if it recurs:**
  1. Just retry (`mvn clean javafx:run` again) — if it's the AV race, it won't repeat twice in a row.
  2. Add a Windows Defender exclusion for the project's `target/` folder (or the whole repo folder) to remove the race entirely.
  3. Avoid running `mvn clean javafx:run` back-to-back with another build/IDE process touching `target/` at the same time.
- **Follow-up:** if this exact error reproduces on a *second* consecutive run with no other file-locking process involved, it stops being an environment issue and needs a real repro (capture `mvn -X` output for the compile phase of that run).

### 0.2 — Native access warning on every launch
- **Status:** Fixed.
- **Symptom:**
  ```
  WARNING: A restricted method in java.lang.System has been called
  WARNING: java.lang.System::load has been called by com.sun.glass.utils.NativeLibLoader in module javafx.graphics
  WARNING: Restricted methods will be blocked in a future release unless native access is enabled
  ```
- **Root cause:** JavaFX's Glass native loader calls the restricted `System::load`; recent JDKs warn (and a future JDK will hard-block it) unless native access is explicitly granted to `javafx.graphics`.
- **Fix:** added `--enable-native-access=javafx.graphics` to the `javafx-maven-plugin` run configuration in [pom.xml](pom.xml) so it's passed to the launched process automatically via `mvn javafx:run`.
- **Note:** this only covers the Maven-launched path. If the app is ever launched via a raw `java -cp ...`/IDE run config outside this plugin, the same flag needs adding there too (or accept the warning — it's non-fatal today).

### 0.3 — `mvn test` fails: `PharmacyServiceImplTest` (2 failures) — pre-existing, unrelated to Developer Dashboard work
- **Status:** Confirmed via test run (2026-08-06) while verifying the Developer Dashboard feature work below. Not caused by that work — `PharmacyServiceImpl.java` and `ValidatorUtils.java` were already modified in the working tree before this session started (see git diff stat: 14 and 140 lines respectively), and neither file was touched while building the bulk-drop/backup/index-comparison/notification features.
- **Symptom:**
  - `addMedication_allowsNullUnitPrice` — fails with an unexpected `IllegalArgumentException: form must not be blank.`
  - `addMedication_throwsValidationException_whenUnitPriceNegative` — expects a `ValidationException` for a negative unit price, gets `IllegalArgumentException: form must not be blank.` instead.
- **Root cause:** [PharmacyServiceImpl.java:45](src/main/java/hospital/management/backend/service/pharmacy/PharmacyServiceImpl.java#L45) calls `ValidatorUtils.requireNonBlank(dto.getForm(), "form")` *before* the unit-price check at line 49 — so any test DTO that doesn't set `form` (these two don't, since form isn't what they're testing) fails on that unrelated check first. Compounding it: `ValidatorUtils.requireNonBlank` throws a raw `IllegalArgumentException`, not the app's typed `ValidationException` — inconsistent with `CreateMedicationDTO`'s own unit-price check three lines down, which correctly throws `ValidationException`.
- **Fix:** either (a) have the test DTOs set a valid `form`, since it's a genuinely required field now, or (b) change `ValidatorUtils.requireNonBlank`/`requireMaxLength` to throw `ValidationException` instead of `IllegalArgumentException` for consistency with the rest of the service layer. Not fixed here — out of scope for the Developer Dashboard feature work in this session.

### 0.5 — "Drop Selected" (indexes/views/routines) silently did nothing on the Developer Dashboard
- **Status:** Fixed.
- **Symptom:** checkboxes select rows fine, but clicking "Drop Selected" for indexes, views, or routines has no visible effect at all — no confirm dialog, no error, no change.
- **Root cause:** [BasePageController.confirm()](src/main/java/hospital/management/pages/BasePageController.java#L42-L44) is a no-op when `confirmModalController` is `null`:
  ```java
  protected void confirm(String title, String body, Runnable onConfirm) {
      if (confirmModalController != null) confirmModalController.show(title, body, onConfirm);
  }
  ```
  `confirmModalController` is `@FXML`-injected from an `<fx:include fx:id="confirmModal" source=".../modal.fxml"/>` — every other page that calls `confirm(...)` (e.g. `retention-settings.fxml`) has this include. **`developer-dashboard.fxml` never did**, so `confirmModalController` was always `null` on this page, and all three drop actions — which all route through `confirm(...)` — silently no-op'd. Pre-existing: this include was missing before this session's changes too; the original single-select "Drop Selected" buttons called `confirm()` the exact same way and would have had the same bug.
- **Fix:** added the missing `<fx:include fx:id="confirmModal" source="/hospital/management/frontend/components/modal/modal.fxml"/>` to `developer-dashboard.fxml`, matching the pattern used by every other page with a confirm dialog.

### 0.4 — Three more pre-existing `mvn test` failures, surfaced by running the full (unfiltered) suite
- **Status:** Confirmed pre-existing — none touch files modified in this session.
- `PageRouteTest.patients_allowsAdminDoctorReceptionist:30` — asserts `PageRoute.PATIENTS.isAllowedFor(RoleName.ANALYST)` is `false`, but [PageRoute.java](src/main/java/hospital/management/enums/PageRoute.java)'s own `PATIENTS` entry explicitly lists `RoleName.ANALYST` among its allowed roles — the test and the enum have disagreed since before this session (verified: the `PATIENTS` line was untouched by any edit here; only a new `SYSTEM_STATUS` entry was appended at the end of the enum). Fix: either drop `RoleName.ANALYST` from `PATIENTS`'s allowed roles, or fix the test's expectation — need product intent on whether Analysts should see the Patients page.
- `PatientMapperTest.toEntity_copiesCreationFieldsOnly:85` — expects gender `"Male"`, actual mapped value is `"M"`. Pre-existing mapper/test mismatch, unrelated to any file touched this session.
- `PatientFeedbackDAOImplTest.save_defaultsDateSubmitted_whenNoneSupplied:80` — expects a non-null default `date_submitted`, gets `null`. Pre-existing, in the already-rewritten (Mongo→Postgres) `PatientFeedbackDAOImpl` from before this session; not modified here.

---

## 1. Correctness bugs

### 1.1 — DOB validation rejects a birth date of "today"
- **Status:** Open
- **File:** [ValidatorUtils.java:168](src/main/java/hospital/management/backend/utils/ValidatorUtils.java#L168) (also `:177`)
- **Bug:** `isValidDateOfBirth`/`requireValidDateOfBirth` use `dob.isBefore(today)`, which rejects `dob == today`.
- **Failure scenario:** Registering a newborn patient on their actual date of birth throws "Date of birth must be a past date." Called on every create/update via [PatientServiceImpl.java:43](src/main/java/hospital/management/backend/service/patient/PatientServiceImpl.java#L43).
- **Fix:** change the comparison to `!dob.isAfter(today)`.

### 1.2 — Null notification recipients break the recipients query
- **Status:** Open
- **File:** [NotificationServiceImpl.java:41](src/main/java/hospital/management/backend/service/notification/NotificationServiceImpl.java#L41)
- **Bug:** unlike `payload`/`channels`/`status` a few lines below, `recipientsJson` has no null-guard, so a null recipients list serializes to the literal string `"null"` instead of an empty JSON array / SQL `NULL`.
- **Failure scenario:** `listForUser`'s `@>` JSON containment query silently never matches that row, so the notification is created but effectively undeliverable/invisible to its intended recipient.
- **Fix:** mirror the null-guard pattern already used for the other three fields in the same method.

### 1.3 — Missing null guard in two new patient-detail dialogs
- **Status:** Open
- **File:** [PatientDetailController.java:973](src/main/java/hospital/management/pages/patient/PatientDetailController.java#L973) (`openAddNoteDialog`) and [:1032](src/main/java/hospital/management/pages/patient/PatientDetailController.java#L1032) (`openSubmitFeedbackDialog`)
- **Bug:** both dereference `currentPatient.getPatientId()` with no null check. The sibling `openAllergyDialog`, added in the same diff, explicitly checks `currentPatient == null` first.
- **Failure scenario:** opening either dialog before/without a loaded patient (e.g. a dangling reference after navigation) throws an unhandled NPE instead of a graceful no-op/error.
- **Fix:** add the same `currentPatient == null` guard used in `openAllergyDialog`.

### 1.4 — `PatientFeedbackDAOImpl.save()` NPEs on a null rating instead of failing cleanly
- **Status:** Open (low priority — currently masked by caller validation)
- **File:** [PatientFeedbackDAOImpl.java:33](src/main/java/hospital/management/backend/dao/patient/PatientFeedbackDAOImpl.java#L33)
- **Bug:** `ps.setInt(4, feedback.getRating())` unboxes a boxed `Integer`. A null rating throws a bare `NullPointerException`, which isn't a `SQLException`, so it bypasses the DAO's usual exception-wrapping and propagates raw.
- **Failure scenario:** today `PatientFeedbackServiceImpl.submitFeedback()` validates `rating != null` before calling `save()`, so this is latent — but any other caller (future service, test, bulk import) that skips that check gets a rogue NPE instead of a typed app exception.
- **Fix:** validate `rating != null` inside the DAO too, or wrap the unboxing in the existing try/catch and rethrow as the DAO's standard exception type.

---

## 2. Architecture / layering violations (per `CLAUDE.md`)

### 2.1 — Raw SQL/JDBC embedded directly in `NotificationServiceImpl`
- **Status:** Open
- **File:** [NotificationServiceImpl.java:46](src/main/java/hospital/management/backend/service/notification/NotificationServiceImpl.java#L46)
- **Issue:** `createNotification`/`listForUser` run SQL directly from the service, bypassing the DAO layer every other domain uses (`Services → DAOs → Postgres`).
- **Fix:** extract a `NotificationDAO`/`NotificationDAOImpl` and move the JDBC there.

### 2.2 — Raw JDBC `Connection` opened directly inside a page controller
- **Status:** Open
- **File:** `DeveloperDashboardController` (new, under [pages/developer](src/main/java/hospital/management/pages/developer)), ~line 546, calls `DBConnection.getConnection()` directly.
- **Issue:** the only file under `pages/` that touches the DB layer directly — violates "the UI layer never calls DAOs directly."
- **Fix:** route through a service (e.g. `DatabaseInspectionService`, already added in this diff) instead of opening a connection in the controller.

### 2.3 — Synchronous DB/Mongo/Redis pings block the FX thread on page load
- **Status:** Open
- **File:** `DeveloperDashboardController.loadSystemInfo()` / `refreshStatusCards()`, ~lines 129-130.
- **Issue:** run synchronously on the JavaFX Application Thread, unlike the sibling `loadDbObjects`/`runBenchmark` in the same class, which correctly go through `AsyncJobRunner`.
- **Failure scenario:** a slow or unreachable datastore freezes the entire UI on opening the dashboard.
- **Fix:** move these calls onto `AsyncJobRunner` like the rest of the class.

### 2.4 — New RBAC permissions (`patient_notes`, `notifications`) granted to Admin only
- **Status:** Open
- **File:** [hospital_rbac_seed_postgresql.sql](src/main/resources/hospital/management/sql/hospital_rbac_seed_postgresql.sql) PART 3, ~lines 118-185.
- **Issue:** Doctor/Receptionist/Analyst/Pharmacist role blocks were not updated with the new `patient_notes`/`notifications` resource-action pairs; only Admin gets them via the wildcard grant.
- **Failure scenario:** once `PermissionGate` is wired to gate the new Mongo-backed patient notes / notifications features, every non-admin seed account (`doctor@hms.com`, etc.) is denied access to a feature clearly meant for them.
- **Fix:** add the appropriate `(resource, action)` grants to the relevant role blocks.

---

## 3. SQL / schema hygiene

### 3.1 — `hospital_migration_v2.sql` duplicates DDL already merged into `hospital_schema.sql`
- **Status:** Open
- **Files:** [hospital_schema.sql:416-467](src/main/resources/hospital/management/sql/hospital_schema.sql#L416-L467) vs. the entirety of [hospital_migration_v2.sql](src/main/resources/hospital/management/sql/hospital_migration_v2.sql)
- **Issue:** both create the `patient_notes`/`notifications` tables, both add `patient_feedback.submitted_by` + its FK + index, both drop the `NOT NULL` on `patient_feedback.patient_id`. Every statement is individually idempotent (`IF NOT EXISTS`), so nothing crashes — but there are now two sources of truth for the same DDL that must be hand-synced forever.
- **Fix:** decide whether `hospital_migration_v2.sql` is for upgrading *pre-v2* production databases (in which case document that explicitly and don't also bake the same DDL into the base schema file), or delete it if the base schema file is the only supported install path.

### 3.2 — Same RBAC permission INSERTs duplicated in `hospital_rbac_seed_postgresql.sql` and `hospital_migration_v2.sql`
- **Status:** Open
- **Files:** [hospital_rbac_seed_postgresql.sql:108-111](src/main/resources/hospital/management/sql/hospital_rbac_seed_postgresql.sql#L108-L111), [hospital_migration_v2.sql:88-97](src/main/resources/hospital/management/sql/hospital_migration_v2.sql#L88-L97)
- **Issue:** same duplicate-source-of-truth problem as 3.1, for the 8 new permission rows.
- **Fix:** same as 3.1 — pick one location.

### 3.3 — MongoDB driver major-version bump folded into an unrelated diff
- **Status:** Informational — no action required now
- **File:** [pom.xml](pom.xml), `mongodb-driver-sync` 4.10.2 → 5.1.4, done as remove/re-add rather than a version edit (easy to miss in review since the block was also relocated).
- **Note:** checked all current call sites (`MongoConfig`, `MongoBenchmarkService`, `PatientNotesMongoService`, `MongoLogStore`, `MongoNotificationStore`) — only stable 4.x/5.x-compatible APIs used, no confirmed break. Flag explicitly in the PR description so reviewers don't miss the major bump.

---

## 4. Design gaps (follow-up tickets, not blockers)

- **`QueryBuilder`** ([QueryBuilder.java:102-165](src/main/java/hospital/management/backend/utils/QueryBuilder.java#L102-L165)) string-concatenates predicate fragments with no guard against a future caller passing unbound user input instead of a `?` placeholder. Safe today (every DAO binds correctly) but a latent SQL-injection trap for the next person who uses it carelessly.
- **`DatabaseInspectionService`** (`dropIndex`/`dropView`/`dropRoutine`, ~lines 131-142/196-206/269-280) builds DDL via string concatenation after only escaping embedded quotes. Safe today (names are catalog-sourced from a `TableView` selection), same class of latent risk as above if ever exposed to free text.
- **`AlgorithmUtils.binarySearch`** (~line 100) NPEs on a null key field instead of returning `-1`; untested edge case (`AlgorithmUtilsTest` doesn't cover it).
- **`PatientNotesNoSqlService`** has no update/delete method despite its read queries filtering `deleted_at IS NULL` — a bad note can never be corrected or soft-deleted once created.
- **`DoctorServiceImpl.findDoctorInCachedDepartment()`** (~line 132) is dead code — not on the `DoctorService` interface, not called anywhere.
- **`MongoConfig`**'s client is never explicitly closed on app shutdown (`Main.stop()` only stops `DatabaseCleanupDaemon`). Low severity — `MongoClients.create()` is lazy and rarely throws — but worth adding to a clean shutdown hook.
- **`DeveloperDashboardController`**'s bulk "Regenerate All Indexes/Views/Routines" actions (~lines 140-146) skip the confirmation dialog that the adjacent single-object drop actions correctly use for the same class of destructive operation.
- **`DoctorsPageController.java:524`** (pre-existing, not part of this diff) — `findUserByEmail()` calls `userDAO.findByEmail()` directly from a page controller; the one other direct-DAO call found across the whole `pages/` tree.

---

## 5. Cosmetic / docs

- [docs/PERFORMANCE_REPORT.md:2](docs/PERFORMANCE_REPORT.md#L2) — bare URL glued directly under the H1 title with no markdown link or spacing.
- `docs/PERFORMANCE_REPORT.md` now states specific benchmark numbers ("10–40x higher throughput", checklist items flipped to `[x]`) with no evidence trail in this diff — confirm with whoever ran `PerformanceBenchmarkService` that these are real captured measurements, not placeholders.

---

## Suggested fix order

1. **0.2** (done) → **1.1**, **1.2**, **1.3** — quick, isolated, high-value correctness fixes.
2. **2.4** — RBAC grant gap, before the new note/notification features ship to non-admin roles.
3. **2.1**, **2.2**, **2.3** — layering cleanup, slightly larger but contained.
4. **3.1**, **3.2** — pick one schema source of truth before this branch merges, to avoid drift.
5. **1.4**, section 4 items — as follow-up tickets, not blockers for this branch.
