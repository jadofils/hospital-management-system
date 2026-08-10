# HMS Backend Utilities — Learning Guide

Supplement to `BACKEND_ARCHITECTURE.md`. Covers everything added after that document:
security classes, DB split, pagination, filters, listeners, and pipes.

---

## 1. Security Package (`config/security/`)

### Why a Separate Security Package?

`EncryptionConfig`, `JwtConfig`, and `PasswordConfig` all deal with the security boundary
of the application. Grouping them makes it obvious where to look when auditing security,
and prevents them from being mixed with infrastructure config like SMTP or Cloudinary.

### `SessionManager` — Who Is Logged In Right Now?

In a desktop JavaFX app one person uses the app at a time. `SessionManager` is the
single place in memory that knows who that person is. Without it, you would pass
`userId` and `role` as method arguments through every controller and service method.

```java
// AuthPageController calls this on successful login:
SessionManager.login(token);   // extracts userId, username, role from the JWE token

// Any controller can then ask:
String userId = SessionManager.getCurrentUserId();    // throws if not logged in
String role   = SessionManager.getCurrentRole();

// Logout button:
SessionManager.logout();   // clears all fields
```

`isLoggedIn()` checks two things: is a token stored AND is it not expired?
If the token has expired mid-session, `isLoggedIn()` calls `logout()` automatically
and returns false — the controller then redirects to the login page.

### `AccessControl` — Can This Role Do That?

Instead of writing `if (role.equals("admin"))` in ten controllers, you write one line:

```java
// Throws ForbiddenException if current role is not admin or doctor:
AccessControl.require(RoleName.ADMIN, RoleName.DOCTOR);

// Boolean check for conditional UI (show/hide a button):
deleteBtn.setDisable(!AccessControl.isAdmin());
```

This matters for maintainability: if the role name changes in the DB, you fix
`RoleName` enum once. If the permission rule changes, you change the one `require()`
call in the controller — not a string comparison scattered across 10 files.

---

## 2. Database Package (`config/db/`)

### `DBConfig` vs `DBConnection` — Why Two Classes?

Before, `DBConnection` owned both WHAT the connection looks like AND HOW the pool runs.
Mixing these two responsibilities means: changing a timeout requires reading pool-management
code; changing the pool library requires reading timeout config.

After splitting:

| Class | Responsibility | Changes when |
|---|---|---|
| `DBConfig` | Pool constants + credential delegates | Timeouts/sizes change, or new pool setting added |
| `DBConnection` | Pool lifecycle, `getConnection()`, `isHealthy()` | Pool library changes (e.g. swap HikariCP) |

```java
// DBConfig owns the numbers:
public static final int  MAX_POOL_SIZE      = 10;
public static final long IDLE_TIMEOUT_MS    = 30_000L;
public static final long MAX_LIFETIME_MS    = 1_800_000L;

// DBConnection uses them:
cfg.setMaximumPoolSize(DBConfig.MAX_POOL_SIZE);
cfg.setIdleTimeout(DBConfig.IDLE_TIMEOUT_MS);
```

### `isHealthy()` — Startup Health Check

```java
if (!DBConnection.isHealthy()) {
    // show "Cannot connect to database" dialog before the main window opens
}
```

Borrows a connection, calls `connection.isValid(2)` (2-second timeout), and returns
true/false without throwing. Use this in `Main.start()` before loading any FXML.

---

## 3. Filters (`utils/filters/`)

### The Three Pieces

**`EntityFilter<T>`** — a `@FunctionalInterface` that tests one item and returns
true (keep) or false (discard). Like `java.util.function.Predicate` but named for
domain clarity and has built-in `.and()`, `.or()`, `.negate()`.

**`FilterBuilder<T>`** — composes multiple `EntityFilter`s fluently. Use for in-memory
filtering of a JavaFX `ObservableList` or `List`.

**`SqlFilterBuilder`** — builds a parameterized SQL `WHERE` clause. Use in DAOs.
Values are always bound as `?` parameters — never interpolated — so SQL injection
is structurally impossible.

### In-Memory vs SQL Filtering

Use **`FilterBuilder`** when:
- The full dataset is already in memory (e.g. a short lookup list)
- Filtering a JavaFX `ObservableList` for a search box

Use **`SqlFilterBuilder`** when:
- Querying the database — let PostgreSQL filter before rows cross the network
- The table has thousands of rows

Both can coexist: SQL narrows the dataset, FilterBuilder refines the result in memory.

### Examples

```java
// ── In-memory (FilterBuilder) ─────────────────────────────────────────────
List<Patient> result = FilterBuilder.<Patient>all()
    .andRaw(() -> !p.isDeleted())               // always active patients
    .andIfPresent(searchText,
        text -> p -> p.getFullName().toLowerCase().contains(text.toLowerCase()))
    .andIfPresent(genderFilter,
        g -> p -> g.equalsIgnoreCase(p.getGender()))
    .apply(allPatients);

tableItems.setAll(result);

// ── SQL (SqlFilterBuilder) ────────────────────────────────────────────────
SqlFilterBuilder filter = SqlFilterBuilder.start()
    .andRaw("deleted_at IS NULL")
    .andEquals("gender", selectedGender)        // null → not added to SQL
    .andLike("first_name || ' ' || last_name", searchText)  // null/blank → skipped
    .andFrom("created_at", fromDate)
    .andTo("created_at", toDate);

String sql = "SELECT * FROM patients "
           + filter.buildWhere()
           + CursorPagination.orderClause(req, "created_at")
           + " LIMIT ?";

List<Object> params = filter.getParams();
params.add(req.getPageSize() + 1);   // fetch one extra to detect hasMore
```

### `andIfPresent()` — Why It Exists

Without it every filter check needs a null guard:

```java
// Without andIfPresent — repetitive:
.and(searchText == null || searchText.isBlank()
     ? null
     : p -> p.getFullName().contains(searchText))

// With andIfPresent — clean:
.andIfPresent(searchText, text -> p -> p.getFullName().contains(text))
```

---

## 4. Pagination (`utils/pagination/`)

### Cursor vs Offset — The Core Problem

```sql
-- OFFSET pagination (fragile):
SELECT * FROM patients ORDER BY created_at DESC LIMIT 20 OFFSET 40;
-- If a new patient is inserted before row 40, you will see a duplicate on page 3.

-- Cursor pagination (stable):
SELECT * FROM patients WHERE created_at < '2024-01-15T10:00:00'
ORDER BY created_at DESC LIMIT 21;
-- The cursor anchors to a specific point. Inserts do not shift it.
```

### The Three Classes

**`PageRequest`** — input. Holds the opaque cursor string (null on first page),
the page size, and sort direction.

**`PageResult<T>`** — output. Holds the items list, the next cursor (null if no more
pages), a `hasMore()` boolean, and the item count.

**`CursorPagination`** — static helpers that build both the SQL fragments and the
`PageResult` from a raw row list.

### How a DAO Uses It

```java
public PageResult<Patient> getAll(PageRequest req) throws DatabaseException {
    SqlFilterBuilder filter = SqlFilterBuilder.start()
        .andRaw("deleted_at IS NULL")
        .andRaw(CursorPagination.whereClause(req, "created_at"));  // cursor constraint

    String sql = "SELECT * FROM patients "
               + filter.buildWhere()
               + CursorPagination.orderClause(req, "created_at")
               + " LIMIT ?";

    List<Object> params = filter.getParams();
    params.add(req.getPageSize() + 1);   // fetch one extra row to detect hasMore

    List<Patient> rows = executeQuery(sql, params);
    return CursorPagination.toResult(rows, req, Patient::getCreatedAt);
}
```

### How a Controller Uses It

```java
private String nextCursor = null;

private void loadPage(boolean firstPage) {
    PageRequest req = firstPage
        ? CursorPagination.firstPage()
        : CursorPagination.nextPage(nextCursor);

    AsyncJobRunner.submit(
        () -> patientService.getAll(req),
        result -> {
            tableItems.setAll(result.getItems());
            nextCursor = result.getNextCursor();
            nextBtn.setDisable(!result.hasMore());
        }
    );
}
```

---

## 5. Listeners (`utils/listeners/`)

### What the EventBus Solves

Without it, when a patient is saved the service would need to directly call every UI
component that wants to know: `dashboardController.refresh()`,
`statsWidget.updateCount()`, etc. The service layer would import JavaFX controllers —
a clean architecture violation.

With EventBus the service just publishes. It knows nothing about who listens.

```
PatientService.save(patient)
  → EventBus.publish(PATIENT_CREATED, patient)
      → DashboardController listener: refreshPatientCount()    ← no import of PatientService
      → StatsWidgetController listener: updateChart()          ← no import of PatientService
```

### Thread Safety

Listeners are always invoked on the JavaFX Application Thread.
If the publisher is on a background thread (e.g. inside `AsyncJobRunner`),
`EventBus` calls `Platform.runLater()` automatically. Listeners can safely
call any JavaFX control without an `isFxApplicationThread()` guard.

### Subscribe and Unsubscribe Pattern

```java
// In DashboardController.initialize():
private Consumer<AppEvent> onPatientCreated = e -> refreshCount();
EventBus.subscribe(AppEventType.PATIENT_CREATED, onPatientCreated);

// In DashboardController — called when navigating away:
EventBus.unsubscribe(AppEventType.PATIENT_CREATED, onPatientCreated);
```

Always unsubscribe when a controller is no longer on screen. If you forget,
the listener keeps a reference to the controller, preventing garbage collection
(a memory leak) and continuing to run even after the page is gone.

---

## 6. Pipes (`utils/pipes/`)

### The Three Pieces

**`DataPipe<T, R>`** — a `@FunctionalInterface`: takes T, returns R, may throw.
One transformation step.

**`PipelineRunner<I, O>`** — chains multiple `DataPipe` steps. Output of step N
is input to step N+1. `runAll()` processes a list and skips items that throw.

**`AsyncJobRunner`** — submits work to a background thread pool so JavaFX stays
responsive. Two modes:

| Method | Use case |
|---|---|
| `submit(job, onSuccess)` | Any single background operation |
| `clean(items, pipe, onDone)` | Batch processing with progress reporting |

### How Pipes, Filters, and Listeners Work Together

```java
// 1. PIPE — define the cleaning transformation
PipelineRunner<String, String> cleanPipe = PipelineRunner.<String>start()
    .pipe(SanitizeUtils::clean)                    // trim + strip control chars
    .pipe(s -> s.replaceAll("\\s{2,}", " "))       // collapse double spaces
    .pipe(s -> WordUtils.capitalize(s));            // proper casing

// 2. FILTER — keep only rows worth saving
EntityFilter<String> nonEmpty = s -> s != null && !s.isBlank();

// 3. LISTENER — react when done
EventBus.subscribe(AppEventType.DATA_CLEANING_COMPLETED,
    e -> {
        List<String> cleaned = e.getPayloadAs(List.class);
        statusLabel.setText("Cleaned " + cleaned.size() + " records.");
    });

// 4. RUN — off the UI thread, progress fires through EventBus
List<String> toClean = FilterBuilder.<String>all()
    .and(nonEmpty)
    .apply(rawNames);

AsyncJobRunner.clean(toClean, cleanPipe::run, cleaned -> {
    patientService.bulkUpdateNames(cleaned);
});
```

---

## Quick Reference: Full Utils Layout

```
backend/utils/
├── SanitizeUtils.java              masks PII for logs, cleans input
├── ValidatorUtils.java             requireNonBlank, requireRange, isValidEmail
│
├── filters/
│   ├── EntityFilter.java           @FunctionalInterface: T → boolean (keep/discard)
│   ├── FilterBuilder.java          composes EntityFilters; applies to a List
│   └── SqlFilterBuilder.java       builds parameterized SQL WHERE clause for DAOs
│
├── pagination/
│   ├── PageRequest.java            input: cursor + pageSize + direction
│   ├── PageResult.java             output: items + nextCursor + hasMore
│   └── CursorPagination.java       firstPage(), nextPage(), whereClause(), toResult()
│
├── listeners/
│   ├── AppEventType.java           enum of all domain events
│   ├── AppEvent.java               type + payload + timestamp
│   └── EventBus.java               subscribe(), publish(), unsubscribe()
│
└── pipes/
    ├── DataPipe.java               @FunctionalInterface: T → R (transforms)
    ├── PipelineRunner.java         chains DataPipes; runAll() skips failures
    └── AsyncJobRunner.java         submit() for async ops; clean() for batch jobs
```

## Pipes vs Filters vs Listeners — Summary Table

| | Input → Output | Caller gets result? | Who knows who? | Use for |
|---|---|---|---|---|
| **Pipe** | T → R (transforms) | Yes, immediately | Caller knows pipe | Clean/convert data |
| **Filter** | T → boolean (selects) | Yes — subset | Caller knows filter | Search, active-only |
| **Listener** | Event → void (reacts) | No | Publisher knows nobody | UI refresh, logging |