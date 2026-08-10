DE.MDde# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build
mvn clean install

# Run application
mvn clean javafx:run

# Run all tests
mvn test

# Run single test class or method
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName

# Build without tests
mvn clean install -DskipTests

# CI / headless Linux
xvfb-run --auto-servernum mvn -B test
```

## Local Setup

1. Install PostgreSQL, Redis, and MongoDB (or run them in Docker)
2. Create a `.env` file in the project root (see `.env.example` if present, or reference `EnvConfig.java`)
3. Execute the SQL scripts in order:
   - `src/main/resources/hospital/management/sql/hospital_schema.sql`
   - `src/main/resources/hospital/management/sql/hospital_objects.sql`
   - `src/main/resources/hospital/management/sql/hospital_rbac_seed_postgresql.sql`
   - `src/main/resources/hospital/management/sql/hospital_indexes_postgresql.sql`
4. Run `mvn clean javafx:run`

**Required `.env` keys:** `DB_URL`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `MONGO_URL`, `JWT_SECRET`, `ENCRYPTION_KEY`, `BCRYPT_ROUNDS`, `GMAIL_*`, `CLOUDINARY_*`, `APP_PAGE_SIZE`, `APP_MAX_UPLOAD_SIZE_MB`

PostgreSQL requires the `pgcrypto` extension (enabled automatically by the schema script). Redis and MongoDB degrade gracefully if unavailable during development.

**Seed accounts** (all use password `Password@12`): `admin@hms.com`, `doctor@hms.com`, `receptionist@hms.com`, `analyst@hms.com`, `pharmacist@hms.com`

## Architecture Overview

This is a **JavaFX desktop application** (not a web app) built on Java 25 with PostgreSQL as the primary database, Redis for caching, and MongoDB for logs/notifications.

### Layered Architecture

```
Pages (JavaFX UI) → Services (business logic) → DAOs (JDBC) → PostgreSQL
                                              ↘ Cache (Redis L2, in-process L1)
                                              ↘ MongoDB (logs, notifications)
```

The UI layer never calls DAOs directly — all reads and writes go through services.

### Package Layout (`src/main/java/hospital/management/`)

| Package | Purpose |
|---|---|
| `pages/` | JavaFX page controllers (one per screen) |
| `pages/components/` | Reusable UI component controllers, organized by domain |
| `backend/config/` | Singleton infrastructure: DB pool, Redis, JWT, encryption, mail, Cloudinary |
| `backend/model/` | Domain entities with base interfaces (Auditable, SoftDeletable, etc.) |
| `backend/dto/` | Request/response DTOs (separate create/update/list variants per domain) |
| `backend/mapper/` | Entity ↔ DTO converters (`DomainMapper`, `DomainSummaryMapper`) |
| `backend/dao/` | JDBC DAOs — interface + implementation pairs per domain |
| `backend/service/` | Business logic — interface + implementation pairs per domain |
| `backend/cache/` | L1 (in-process Map) + L2 (Redis) with eviction and invalidation |
| `backend/daemon/` | Background cleanup tasks (retention policies, log archival) |
| `backend/utils/` | Stateless utilities: validators, sanitizers, filters, pagination, EventBus, pipelines |
| `backend/exceptions/` | Typed exception hierarchy rooted at `AppException` |

### Key Design Decisions

**Configuration:** All `config/` classes are static-initializer singletons that fail fast on startup. `EnvConfig` is the single source of truth for `.env` values. Never read environment variables directly — go through `EnvConfig`.

**Security:**
- Passwords use BCrypt (cost 12) via `PasswordConfig`. Never compare hashes directly.
- Auth tokens are JWE (encrypted), not JWS (signed-only) — payload is opaque to clients.
- `EncryptionConfig` derives two independent AES-256-GCM keys from one master secret for domain separation.
- Two-layer RBAC: application-level via `PermissionGate` + PostgreSQL-level role grants.

**Database:**
- All tables use UUID PKs via `gen_random_uuid()` (pgcrypto).
- Soft delete everywhere — `deleted_at` timestamp, never hard delete on patient-facing data.
- DAOs use `PreparedStatement` for all queries (no string interpolation).
- The `invoices` table with `payment_status='paid'` serves as the receipt record (no separate receipts table).

**Caching:** Cache invalidation is enforced before every write in the service layer. L1 (in-process) is checked first; L2 (Redis) on miss. Cache keys use random UUIDs in tests to prevent inter-test leakage.

**Events:** `EventBus` (in `utils/listeners/`) provides in-process pub/sub for decoupling services from UI updates and cache invalidation.

**Testing split:**
- DAO tests (301) hit a real PostgreSQL instance — no mocks for persistence.
- Service tests (256) use Mockito-mocked DAOs — no DB required.
- `EventBusTest` requires a JavaFX Platform thread (needs Xvfb in headless CI).
- `TransactionManager` tests use `Mockito.mockStatic()` to intercept static JDBC calls.

## Documentation

Detailed design docs live in `/docs/`:
- `DATABASE.md` — All 25 tables, 35+ indexes, views, triggers, stored procedures
- `BACKEND_ARCHITECTURE.md` — Config layer, fail-fast patterns, key derivation
- `BACKEND_DATA_LAYER.md` — DAO patterns and integration testing approach
- `TESTING_REPORT.md` — Full 744-test breakdown
- `hmserd.pdf` — Entity Relationship Diagram
