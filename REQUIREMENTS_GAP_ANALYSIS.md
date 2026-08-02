# Requirements Gap Analysis — vs. `README.md`

**Date:** 2026-08-02
**Context:** `README.md` at the repo root is the original course/assignment brief (Epics, User Stories, Technical Requirements, Deliverables, Evaluation Criteria) — not a setup guide. This document compares what has actually been built and tested against that brief, deliverable by deliverable, so it's clear what's done, what's partial, and what's genuinely missing.

---

## Scorecard at a glance

| # | Deliverable (per README) | Status |
|---|---|---|
| 1 | Database Design Document (conceptual/logical/physical ERD) | ⚠️ Partial |
| 2 | SQL scripts: schema, constraints, indexes, sample data | ⚠️ Partial |
| 3 | JavaFX + JDBC: CRUD, search, reporting | ✅ Done |
| 4a | DSA — Caching | ✅ Done |
| 4b | DSA — In-memory sorting/searching algorithms | ❌ Missing |
| 4c | DSA — Performance Report (before/after optimization) | ❌ Missing |
| 5 | NoSQL design (optional) | ❌ Not attempted |
| 6 | Setup/usage README (separate from the brief) | ⚠️ Partial |
| 7 | Testing Evidence | ✅ Done (this pass) |

---

## 1. Database Design Document — ⚠️ Partial

**Required:** conceptual ERD, logical model (attributes/PKs/FKs), physical model (SQL types, constraints, 3NF), all diagrammed.

**What exists:** `DATABASE.md` — a thorough 567-line **textual** reference: all 25 tables, every column, constraint, index, view, stored procedure, trigger, DCL role, and the Java-model mapping.

**What's missing:**
- No ERD diagram anywhere in the repo — no `.drawio`, `.puml`, `.dbml`, `.png`, `.pdf`, nothing. `DATABASE.md` describes relationships in prose/tables, not as a visual graph.
- No explicit staged presentation of conceptual → logical → physical, as the brief's User Story 1.1 asks for — the schema is presented once, at the physical level.

**Recommendation:** generate an ERD (even a single `dbdiagram.io`/`drawio` export committed as PNG + source file would satisfy this) and a short conceptual-model section in `DATABASE.md` or a new `docs/erd.md`.

---

## 2. SQL Implementation Scripts — ⚠️ Partial

**Required:** schema creation, constraints, indexes, and **sample data**.

**What exists**, under `src/main/resources/hospital/management/sql/`:
- `hospital_schema.sql` (531 lines) — all 25 tables, constraints, base indexes, `updated_at` triggers.
- `hospital_objects.sql` (491 lines) — views, stored procedures, business-logic triggers, DCL roles.
- `hospital_indexes_postgresql.sql` (158 lines) — composite indexes, GIN trigram indexes for `ILIKE` search, partial active-record indexes.
- `hospital_rbac_seed_postgresql.sql` (309 lines) — seeds departments (5), doctors (5), roles, permissions, role-permission grants, and 5 user accounts.

**What's missing:** no seed/sample `INSERT` statements for the *clinical* data — patients, appointments, medical_records, prescriptions, prescription_items, medical_inventory, lab_orders, invoices, patient_feedback. The RBAC seed covers identity/access data only. A grader or new developer has no way to see the schema populated with realistic hospital data without using the running app to create it by hand.

**Recommendation:** add a `hospital_sample_data_postgresql.sql` with a modest, realistic dataset (e.g. 20–30 patients, a few weeks of appointments, matching prescriptions/invoices) that FKs cleanly against the RBAC seed's existing doctors/departments.

---

## 3. JavaFX + JDBC: CRUD, search, reporting — ✅ Done

- Full `Controller → Service → DAO` layering (`backend/dao`, `backend/service`, `backend/dto`, `backend/mapper`, `backend/model`; `pages/*` for JavaFX controllers, one subfolder per domain).
- JDBC access is properly parameterized throughout — `SqlFilterBuilder` and `CursorPagination` build `?`-placeholder queries, no string-concatenated user input found anywhere in the DAO layer.
- This pass added 744 automated tests directly exercising this layer (see `TESTING_REPORT.md`), which is itself strong evidence the CRUD/search plumbing works as designed — the 5 real bugs/gaps it surfaced (see that report, §3) are the kind of thing that would otherwise only surface in production.

**No action needed** — this is the best-covered deliverable in the project.

---

## 4. Data Structures & Algorithms

### 4a. Caching — ✅ Done
`backend/cache/`:
- `CacheService` — two-tier facade (in-process L1 → Redis L2), `get`/`set`/`evict`/`evictByPattern`, "delete-before-write" invalidation to avoid write-after-write races.
- `L1Cache` — `ConcurrentHashMap`-backed, LRU eviction via a `Comparator` over last-access time, background sweeper thread for expiry.
- `CacheDomain` — per-domain TTL policy.
- `CacheKey` — centralized, compile-time-safe key construction.

This is a genuine, non-trivial in-memory caching layer, and it's now covered indirectly by every Service test in the suite (each Service test exercises real `CacheService` calls, only the DAO is mocked).

### 4b. In-memory sorting/searching algorithms — ❌ Missing
The brief asks for actual **Java-level** sorting/searching (the point being to relate it to database indexing concepts). What exists instead: all sorting is `ORDER BY` in SQL (`CursorPagination.orderClause`), and all searching is `ILIKE` in SQL (`SqlFilterBuilder.andLike`) — both correct and reasonable *engineering* choices, but they don't fulfill the brief's specific ask for a demonstrated, in-memory algorithm (e.g. a `Comparator`-driven sort over a cached `List`, or a binary search over a sorted in-memory structure) with an explanation of how it relates to a DB index.

**Recommendation:** if this needs to be satisfied literally, add one clearly-labeled example — e.g. sort a cached `List<PatientSummaryDTO>` in-memory with a `Comparator` before display, or binary-search a sorted in-memory list of appointment start-times — with a short doc comment tying it back to how a B-tree index achieves the same speedup on the DB side.

### 4c. Performance Report — ❌ Missing
No before/after timing, no `EXPLAIN ANALYZE` output, no benchmark code or document anywhere in the repo.

**Recommendation:** this is usually the fastest of the missing items to produce — run a representative query (e.g. patient search by name) before and after adding its supporting index, capture `EXPLAIN ANALYZE` output both times, and write up the delta in a short `PERFORMANCE_REPORT.md`.

---

## 5. NoSQL design (optional) — ❌ Not attempted

Redis is present in the stack, but purely as an L2 cache tier (TTL'd entity/list lookups) — not as a document store for unstructured data like patient notes or medical logs, which is what the brief's Epic 4 / User Story 4.2 is actually asking about. `medical_records.notes` and `system_logs.message` remain plain relational `TEXT` columns. No MongoDB or other document-store dependency exists in `pom.xml`.

This is explicitly marked **optional** in the brief — flagging it as not attempted, not as a defect.

---

## 6. Setup/usage README — ⚠️ Partial

**Required:** a README describing setup, dependencies, how to run the SQL scripts, and usage instructions.

**What exists, scattered across multiple files:**
- `CLAUDE.md` — has the build/run commands (`./mvnw clean javafx:run`, Java 25 + JavaFX 21.0.6 requirement) and an architecture overview, but it's explicitly framed as AI-assistant guidance, not a human-facing setup doc.
- `DATABASE.md` — has a "quick start on a fresh database" `psql` snippet, the closest thing to SQL-running instructions that exists.
- Several other root-level `.md` files (`BACKEND_ARCHITECTURE.md`, `BACKEND_DATA_LAYER.md`, `BACKEND_STRUCTURE.md`, `BACKEND_UTILITIES.md`, `development-priority.md`, `home.md`, `RETENTION_SETTINGS_UI.md`) are architecture/reference notes, not setup guides.
- `README.md` itself is the assignment brief, not a setup guide.

**What's missing:** one canonical, human-facing README or `docs/SETUP.md` consolidating: prerequisites (Java 25, JavaFX 21.0.6, Postgres, Redis, Docker for running DAO integration tests), the `.env` variables required (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, etc. — see `EnvConfig.java`), the order to run the SQL scripts in (`hospital_schema.sql` → `hospital_objects.sql` → `hospital_indexes_postgresql.sql` → `hospital_rbac_seed_postgresql.sql`), and how to build/run/test the app.

**Recommendation:** this is a documentation-consolidation task, not new engineering — mostly copy-and-organize from `CLAUDE.md` + `DATABASE.md` into one `SETUP.md`, aimed at a human reader rather than an AI agent.

---

## 7. Testing Evidence — ✅ Done (this pass)

**Required:** screenshots or reports showing correct query results and validation outcomes.

**Before this pass:** zero automated tests existed; no screenshots or written validation evidence either.

**Now:** 744 passing JUnit 5 tests across the entire `backend` layer — see `TESTING_REPORT.md` for the full breakdown, methodology, and the real bugs/gaps the tests surfaced. This satisfies the *spirit* of the deliverable (automated, repeatable proof of correct behavior, arguably stronger evidence than static screenshots) and is now enforced continuously via CI + branch protection on `master`, so it can't silently regress.

**Residual gap:** the brief's literal wording ("screenshots ... showing correct query results") suggests some manual QA evidence was also expected, e.g. screenshots of the running JavaFX app performing CRUD/search/reporting. None exist. If the evaluator wants literal screenshots alongside the automated suite, that's a quick follow-up (run the app, capture a handful of key screens: patient search, appointment booking, invoice view).

---

## Priority-ordered follow-ups

If picking up where this leaves off, roughly in order of effort-to-impact:

1. **Sample data SQL script** (§2) — small effort, closes a real functional gap (nothing to demo without it).
2. **Setup README consolidation** (§6) — small effort, pure documentation.
3. **Performance report** (§4c) — small-to-medium effort, mostly writing up `EXPLAIN ANALYZE` output that's easy to generate against the existing indexes.
4. **ERD diagram** (§1) — medium effort, needs a diagramming tool but the underlying model is already fully documented in `DATABASE.md`.
5. **In-memory sort/search example** (§4b) — small effort, one well-chosen example with a doc comment.
6. **NoSQL design** (§5) — optional; skip unless explicitly required for full marks.
