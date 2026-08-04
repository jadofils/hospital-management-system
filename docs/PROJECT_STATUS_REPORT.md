# Project Status Report

Date: 2026-08-04
Project: Hospital Management System

## 1. Executive Summary

This report maps current repository state to the requirements defined in README.md.

## 2. Requirement Coverage

### 2.1 Database Design and Modeling

- Status: Completed
- Evidence:
  - ERD: hmserd.pdf
  - Conceptual diagrams: CONCEPTUAL_LEVEL_DIAGRAMS.md
  - Logical diagrams: LOGICAL_LEVEL_DIAGRAMS.md
  - Physical diagrams: PHYSICAL_LEVEL_DIAGRAMS.md
  - Root physical SQL model: hmserd-postgresql.sql
  - SQL schema and objects: src/main/resources/hospital/management/sql/hospital_schema.sql, hospital_objects.sql
  - Reference documentation: DATABASE.md

### 2.2 SQL Implementation Script

- Status: Completed
- Evidence:
  - Core schema and constraints: hospital_schema.sql
  - Advanced objects and triggers: hospital_objects.sql
  - Performance indexes: hospital_indexes_postgresql.sql
  - Seed data including domain records: hospital_seed_data.sql
  - RBAC seed: hospital_rbac_seed_postgresql.sql

### 2.3 JavaFX + JDBC Integration

- Status: Completed
- Evidence:
  - Layered structure in src/main/java/hospital/management:
    - pages (controllers)
    - backend/service
    - backend/dao
  - Parameterized SQL and DAO abstractions present across modules.

### 2.4 DSA Integration

- Status: Completed
- Completed:
  - In-memory cache layer with L1/L2 strategy and eviction logic.
  - Case-insensitive patient search in table filters and DAO-level search query paths.
  - Deterministic in-memory sorting during patient report export (last name, first name, id tie-break).
  - Cache invalidation is enforced before writes in services to avoid stale reads.

### 2.5 Patient Report Download Story (Analyst and Receptionist)

- Status: Completed
- Evidence:
  - `PageRoute.PATIENTS` now allows analyst access in addition to admin/doctor/receptionist.
  - Patients page exposes a dedicated "Download Report" action.
  - Report export path produces timestamped CSV files (`patients_report_yyyyMMdd_HHmm.csv`).
  - Report rows are sorted consistently for reliable analyst/receptionist reporting workflows.

### 2.6 Billing Table and Business Logic

- Status: Completed
- Evidence:
  - Physical table: `invoices` with `payment_status` constraint in `hospital_schema.sql`.
  - DAO layer: `InvoiceDAOImpl` provides CRUD, pagination, and payment-status updates.
  - Service layer: `InvoiceServiceImpl` enforces validations, duplicate-by-appointment guard, cache invalidation, and paid transition logic.
  - UI layer: `InvoicePageController` supports invoice creation, status update, delete, CSV export, and report printing.

### 2.7 Performance Optimization Evidence

- Status: In Progress
- Completed:
  - Index scripts and optimization structures exist.
  - Performance report structure finalized in PERFORMANCE_REPORT.md.
- Remaining:
  - Fill benchmark result table with measured latency/P95/throughput values from actual runs.

### 2.8 Testing Evidence

- Status: Completed
- Evidence:
  - TESTING_REPORT.md
  - 744 tests documented as passing in prior report snapshot.

## 3. NoSQL Scope Compliance

- MongoDB usage is scoped to unstructured/semi-structured records and logging/notifications.
- Core transactional entities remain in PostgreSQL.
- Connection variable convention uses MONGO_URL (with compatibility fallback to MONGO_URI).

## 4. Current Risks and Open Items

1. Mongo authentication warnings can occur when MONGO_URL credentials/authSource are incorrect.
2. Performance benchmark table still requires measured values.
3. Additional UI-level automated tests are not yet part of the suite.

## 5. Recommended Next Actions

1. Execute benchmark runs and complete PERFORMANCE_REPORT.md result tables.
2. Add UI smoke test evidence (manual screenshots or automated TestFX checks).
