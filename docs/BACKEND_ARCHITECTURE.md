# HMS Backend Architecture — Learning Guide

A practical reference for understanding how the backend of this JavaFX hospital management
system is structured and why each design decision was made.

---

## 1. Configuration Layer (`backend/config/`)

### The Pattern: Static Initializer Singleton

Every config class uses a `static {}` block instead of a constructor:

```java
public final class JwtConfig {
    private static final SecretKey KEY;

    static {
        KEY = EncryptionConfig.getJwtKey();   // runs once, on first class load
    }

    private JwtConfig() {}   // nobody can call `new JwtConfig()`
}
```

**Why?** Fail-fast startup. If `JWT_SECRET` is too short, the app crashes immediately
with a clear message instead of silently producing broken tokens during a real login.
Configuration errors are a programming mistake — they should stop the program before
any user interaction begins.

### Initialization Order

`EncryptionConfig` → `JwtConfig` (JwtConfig calls `EncryptionConfig.getJwtKey()` in its
static block, which triggers EncryptionConfig to initialize first. Java guarantees this
because the reference itself is the trigger.)

### Each Class and Why It Exists

| Class | What it does | Key design decision |
|---|---|---|
| `EnvConfig` | Reads all `.env` keys | Single source of truth — no other class calls `Dotenv.load()` directly |
| `EncryptionConfig` | AES-256-GCM encrypt/decrypt, key derivation | Domain-separated keys from one master secret |
| `JwtConfig` | Issues and parses JWE (encrypted JWT) tokens | JWE not JWS — payload is encrypted, not just signed |
| `PasswordConfig` | BCrypt hash and verify | Hash always generates a new salt — you can never compare two hashes directly |
| `MailConfig` | Gmail SMTP session via STARTTLS | Session is reused — creating one per email wastes connections |
| `CloudinaryConfig` | Cloudinary SDK singleton | `secure: true` forces HTTPS delivery URLs always |
| `AppConfig` | Window constants + env-backed settings | Hardcoded vs env-backed: constants never change per deploy, settings do |
| `AppLogger` | Thin wrapper around `java.util.logging` | Centralizes log format so switching implementations is one file change |

### EncryptionConfig: Key Derivation Explained

One master secret (`ENCRYPTION_KEY`), two independent keys:

```
SHA-256("data:" + ENCRYPTION_KEY) → 256-bit AES key for encrypt()/decrypt()
SHA-256("jwt:"  + ENCRYPTION_KEY) → 256-bit AES key for JWT payloads
```

Why separate keys from one secret? If someone extracts a JWT token from memory,
they still cannot use the JWT key to decrypt database fields — the keys are
mathematically unrelated even though they come from the same secret.

Wire format of `encrypt()` output:

```
Base64URL( [12 bytes: random IV] [N bytes: ciphertext + 16 bytes: GCM tag] )
```

A fresh random IV is generated on every call. This means encrypting the same plaintext
twice produces different ciphertext — preventing pattern analysis.

### JwtConfig: JWE vs JWS

Most tutorials show JWT as **signed** (JWS). This system uses **encrypted** (JWE):

```
JWS: header.payload.signature     ← payload is BASE64 DECODED — anyone can read it
JWE: header.encrypted_key.iv.ciphertext.tag  ← payload is encrypted — opaque without key
```

Token claims: `sub` (userId), `username`, `role`, `iat`, `exp`.

Parsing throws typed exceptions — never raw JJWT exceptions:
```java
TokenExpiredException  ← expired token — redirect to login
AuthException          ← tampered/invalid token — reject and log
```

---

## 2. Utils Layer (`backend/utils/`)

### Why a Separate Utils Package?

Without it, the same regex for masking emails would appear in `EncryptionConfig`,
`AppLogger`, `AuthPageController`, and any service that logs user data.
One bug fix would require 10 edits. Utils centralizes logic that has no state.

### `SanitizeUtils` — Output and Input Cleaning

**Rule: every value written to a log must go through `maskForLog()` first.**

```java
// WRONG — exposes PII in log files:
logger.info("Login attempt for: " + email);

// CORRECT — PII masked before writing:
logger.info("Login attempt for: " + SanitizeUtils.maskForLog(email));
// Output: "Login attempt for: joh***@exa***.com"
```

| Method | Input | Output |
|---|---|---|
| `maskForLog(message)` | Any string with embedded PII | Masks all emails, UUIDs, phones in-place |
| `maskEmail(email)` | `john.doe@example.com` | `joh***@exa***.com` |
| `maskUuid(uuid)` | `550e8400-e29b-41d4-...` | `550e8400-***` |
| `maskPhone(phone)` | `+1 (555) 867-5309` | `***-***-5309` |
| `clean(input)` | Raw form text | Trimmed + control characters stripped |

### `ValidatorUtils` — Pre-condition Guards

These throw instead of returning boolean so the check and the error message are written
once, not at every call site:

```java
// Without utils — repeated at every entry point:
if (password == null || password.isBlank()) {
    throw new IllegalArgumentException("password must not be blank.");
}

// With utils — one line, same behavior:
ValidatorUtils.requireNonBlank(password, "password");
```

**Why `IllegalStateException` for `requireRange()`?**
Out-of-range BCrypt rounds is a configuration mistake, not user input.
`IllegalStateException` signals "the system is in an invalid state at startup."
`IllegalArgumentException` signals "you passed bad data." Both are `RuntimeException`
subclasses — the distinction is about WHO caused the problem.

---

## 3. Exception Hierarchy (`backend/exceptions/`)

### Checked vs Unchecked — The Decision Rule

| Type | When to use | Examples |
|---|---|---|
| Unchecked (`extends RuntimeException`) | Error is a programming mistake or unrecoverable at the call site | Auth failures, validation errors, encryption errors |
| Checked (`extends Exception`) | Error is expected and the caller MUST decide how to handle it | Database failures (retry? rollback?), mail failures (queue? alert?) |

### The Full Hierarchy

```
RuntimeException
└── AppException                     ← catch everything app-related with one clause
    ├── AuthException                ← credentials wrong, token bad format
    │   ├── TokenExpiredException    ← JWT valid but expired → send to login
    │   ├── UnauthorizedException    ← no session at all → send to login
    │   └── ForbiddenException       ← logged in but role blocked → show "Access Denied"
    ├── ValidationException          ← bad user input; has .getField() for UI highlighting
    ├── ResourceNotFoundException    ← DB lookup empty; has .getResourceType(), .getResourceId()
    ├── EncryptionException          ← AES-GCM failed; likely tampered data
    ├── ConfigurationException       ← bad .env at startup; app cannot continue
    └── FileUploadException          ← size, type, or Cloudinary failure

Exception (checked)
├── DatabaseException                ← SQL/connection failure; caller must handle
└── MailException                    ← SMTP failure; caller must handle
```

### How to Use in a Controller

```java
try {
    String userId = JwtConfig.getUserId(token);
    // ... load data for this user
} catch (TokenExpiredException e) {
    navigateTo(PageRoute.AUTH);          // expired — go to login
} catch (ForbiddenException e) {
    toast.show(e.getMessage(), NotificationType.ERROR);
} catch (AuthException e) {
    toast.show("Session invalid. Please log in again.", NotificationType.ERROR);
} catch (AppException e) {
    toast.show("Unexpected error: " + e.getMessage(), NotificationType.ERROR);
}
```

The hierarchy is deliberate: catching `AuthException` also catches all three of its
subclasses. You choose the level of specificity you need.

### `ValidationException` — UI Tip

```java
try {
    ValidatorUtils.requireValidEmail(emailField.getText(), "email");
} catch (ValidationException e) {
    // e.getField() returns "email" — use it to highlight the control
    emailField.setStyle("-fx-border-color: red;");
    errorLabel.setText(e.getMessage());
}
```

---

## 4. Model OOP and SOLID (`backend/model/base/`)

### The Problem Before

Every model repeated the same three fields independently:

```java
// In Patient.java:
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
private LocalDateTime deletedAt;

// In Doctor.java: (identical)
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
private LocalDateTime deletedAt;

// ... same in User, Appointment, LabOrder, Invoice — 20 times total
```

To add an `isDeleted()` convenience method would mean editing all 20 files.

### The Solution: Abstract Base Hierarchy

```
Interfaces (contract only, no fields):
  Identifiable    — getId(), getEntityType()
  Auditable       — getCreatedAt()
  SoftDeletable   — getDeletedAt(), isDeleted(), markDeleted()

Abstract classes (shared implementation):
  BaseEntity      — implements all three interfaces; owns id, createdAt, updatedAt, deletedAt
    └── Person    — adds firstName, lastName, phone, email; owns getFullName()
  BaseLog         — implements Identifiable + Auditable only (no soft-delete — logs are immutable)

Concrete classes (only what is unique):
  Patient    extends Person    — adds dob, gender, address
  Doctor     extends Person    — adds departmentId, specialization
  User       extends BaseEntity — adds username, passwordHash, email, isActive
  Appointment extends BaseEntity — adds patientId, doctorId, appointmentDate, status, reason
  AuditLog   extends BaseLog   — adds action, tableAffected, recordId
  SystemLog  extends BaseLog   — adds logLevel, source, message
  LabOrder   extends BaseEntity — adds appointmentId, doctorId, testName, status
```

### SOLID Principles Applied

**S — Single Responsibility**
- `BaseEntity` knows about identity and audit timestamps only.
- `Person` knows about personal contact details only.
- `Patient` knows about medical-specific data only.
- Each class has exactly one reason to change.

**O — Open/Closed**
To add a `Nurse` entity: extend `Person`, implement `getEntityType()` and `getDisplayTitle()`.
You do not touch `BaseEntity`, `Person`, or any existing class.

**L — Liskov Substitution**
Any code that accepts `BaseEntity` works correctly with `Patient`, `Doctor`, `Appointment`.
A `List<BaseEntity>` can hold any mix of subtypes without breaking.

**I — Interface Segregation**
`BaseLog` does NOT implement `SoftDeletable`. Log tables have no `deleted_at` column.
If all entities shared one fat interface, `AuditLog` would be forced to implement
`markDeleted()` for a column that doesn't exist in its DB table — a Liskov violation.

**D — Dependency Inversion**
Services and DAOs declare parameters as `BaseEntity` or interface types, not concrete
classes. Example: a generic audit logger works on any entity without knowing its type.

### Polymorphism in Action

`getSummary()` is declared abstract in `BaseEntity` and `BaseLog`. Each concrete class
implements it differently. A single line of code works on any entity type:

```java
public void logEntityChange(BaseEntity entity) {
    // entity.getSummary() dispatches to the right implementation at runtime:
    // Patient  → "Patient: John Doe | john@example.com"
    // Doctor   → "Dr. Jane Smith | Cardiology"
    // Appointment → "Appointment[scheduled] — 2024-01-15 10:00"
    logger.info("Changed: " + SanitizeUtils.maskForLog(entity.getSummary()));
}
```

`getDisplayTitle()` is abstract in `Person`. Both `Patient` and `Doctor` implement it.
`Person.getSummary()` calls `getDisplayTitle()` — this is the **Template Method** pattern:
the base class defines the algorithm structure, subclasses fill in the variable step.

```java
// In Person (abstract):
@Override
public String getSummary() {
    return getDisplayTitle() + " " + getFullName() + " | " + email;
    //     ↑ calls the subclass implementation at runtime
}

// Patient returns "Patient", Doctor returns "Dr."
// — same getSummary() code, different runtime output.
```

### Domain ID Aliases

The DB column is `patient_id` and existing code uses `getPatientId()`. After refactoring,
`id` lives in `BaseEntity`. The alias keeps backward compatibility with `PropertyValueFactory`:

```java
// In Patient.java:
public String getPatientId() { return getId(); }   // alias — no field duplication
public void setPatientId(String id) { setId(id); } // DAO sets via this
```

`PropertyValueFactory<Patient, String>("patientId")` still works because it finds
`getPatientId()` via reflection — which returns `getId()` from the base class.

### LabOrder: Column Name Mismatch

The DB uses `ordered_at` instead of `created_at`. The model maps it through an alias:

```java
// In LabOrder constructor:
setCreatedAt(orderedAt);   // stored in base class field

// Exposed with the DB-matching name:
public LocalDateTime getOrderedAt() { return getCreatedAt(); }
```

One field in one place. Two names for the same concept.

---

## Quick Reference: Where Each Piece Lives

```
backend/
├── config/
│   ├── EnvConfig.java           reads .env — one source of truth
│   ├── EncryptionConfig.java    AES-256-GCM encrypt/decrypt, exports JWT key
│   ├── JwtConfig.java           JWE token issue/parse (userId + username + role)
│   ├── PasswordConfig.java      BCrypt hash/verify
│   ├── MailConfig.java          SMTP session (Gmail STARTTLS port 587)
│   ├── CloudinaryConfig.java    image upload SDK singleton
│   ├── AppConfig.java           UI constants + env-backed settings
│   └── AppLogger.java           logging wrapper
│
├── utils/
│   ├── SanitizeUtils.java       maskForLog, maskEmail/Uuid/Phone, clean
│   └── ValidatorUtils.java      requireNonBlank, requireRange, isValidEmail/Uuid/Phone
│
├── exceptions/
│   ├── AppException.java            unchecked base — catch-all for the app
│   ├── AuthException.java           unchecked — bad credentials / token
│   ├── TokenExpiredException.java   unchecked — JWT expired
│   ├── UnauthorizedException.java   unchecked — no session
│   ├── ForbiddenException.java      unchecked — role denied
│   ├── ValidationException.java     unchecked — bad input (carries field name)
│   ├── ResourceNotFoundException.java unchecked — DB lookup returned nothing
│   ├── EncryptionException.java     unchecked — AES-GCM failure
│   ├── ConfigurationException.java  unchecked — bad .env at startup
│   ├── FileUploadException.java     unchecked — upload rejected
│   ├── DatabaseException.java       CHECKED — SQL/connection failure
│   └── MailException.java           CHECKED — SMTP failure
│
└── model/
    └── base/
        ├── Identifiable.java    interface — getId(), getEntityType()
        ├── Auditable.java       interface — getCreatedAt()
        ├── SoftDeletable.java   interface — getDeletedAt(), isDeleted(), markDeleted()
        ├── BaseEntity.java      abstract — all soft-deletable persistent entities
        ├── Person.java          abstract — Patient, Doctor (has name/phone/email)
        └── BaseLog.java         abstract — AuditLog, SystemLog (append-only, no soft-delete)
```