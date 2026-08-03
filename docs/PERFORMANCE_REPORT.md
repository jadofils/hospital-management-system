# Performance Report: Relational vs NoSQL for Unstructured Clinical Data

## 1. Report Metadata
- Project: Hospital/Healthcare Management System
- Date: 2026-08-03
- Environment:
  - Java: 25
  - Maven: 3.8.5 (wrapper)
  - Relational DB: PostgreSQL
  - NoSQL DB: MongoDB
- Dataset references:
  - SQL seed: src/main/resources/hospital/management/sql/hospital_seed_data.sql
  - Import samples: imports/patients, imports/appointments, imports/pharmacy

## 2. Objective
Compare relational and NoSQL approaches for unstructured/semi-structured healthcare data and document optimization impact before vs after indexing.

## 3. Scope and Boundaries
- PostgreSQL remains source of truth for transactional entities.
- MongoDB is used for unstructured/semi-structured data and operational logs.
- Comparison targets:
  - Patient notes
  - Medical/system logs
  - Notification payloads

## 4. Data Models Compared

### 4.1 PostgreSQL Model
- Core schema and constraints: src/main/resources/hospital/management/sql/hospital_schema.sql
- Performance indexes: src/main/resources/hospital/management/sql/hospital_indexes_postgresql.sql

### 4.2 MongoDB Model
- URI source: MONGO_URL (fallback MONGO_URI) from EnvConfig
- Operational logging collections are used for benchmark logging paths.

## 5. Benchmark Methodology
- Workload shape: mixed read/write for logs and unstructured records.
- Baseline: run before index-focused queries.
- Optimized: run with current index scripts and query paths.
- Measurements to capture per operation:
  - Average latency (ms)
  - P95 latency (ms)
  - Throughput (ops/s)

## 6. Test Operations
1. Insert single unstructured record
2. Insert batch records
3. Fetch by patientId plus time range
4. Fetch by keyword
5. Fetch recent N records
6. Update metadata fields

## 7. Baseline and Optimized Results

| Operation | Store | Avg Before (ms) | Avg After (ms) | P95 Before (ms) | P95 After (ms) | Throughput Before (ops/s) | Throughput After (ops/s) | Improvement % |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| Insert single | PostgreSQL | pending | pending | pending | pending | pending | pending | pending |
| Insert single | MongoDB | pending | pending | pending | pending | pending | pending | pending |
| Insert batch | PostgreSQL | pending | pending | pending | pending | pending | pending | pending |
| Insert batch | MongoDB | pending | pending | pending | pending | pending | pending | pending |
| Fetch by patient+time | PostgreSQL | pending | pending | pending | pending | pending | pending | pending |
| Fetch by patient+time | MongoDB | pending | pending | pending | pending | pending | pending | pending |
| Keyword search | PostgreSQL | pending | pending | pending | pending | pending | pending | pending |
| Keyword search | MongoDB | pending | pending | pending | pending | pending | pending | pending |
| Recent N | PostgreSQL | pending | pending | pending | pending | pending | pending | pending |
| Recent N | MongoDB | pending | pending | pending | pending | pending | pending | pending |

## 8. Optimizations Applied

### PostgreSQL
- Composite and partial indexes from hospital_indexes_postgresql.sql
- Trigram indexes for text-like searches where applicable
- Cursor-style pagination patterns in backend utilities

### MongoDB
- Structured write/read paths for system and audit log style documents
- Collection-level query patterns for latest-first retrieval

## 9. Analysis (Current)
- Structural optimization work is in place.
- Measurement capture is still pending execution data for final numerical conclusions.

## 10. Conclusion (Current)
- Architecture follows the intended hybrid model: PostgreSQL for transactions, MongoDB for unstructured/log-like data.
- Final performance claims require benchmark run outputs to replace pending values.

## 11. Evidence Appendix
- README.md
- DATABASE.md
- src/main/resources/hospital/management/sql/hospital_schema.sql
- src/main/resources/hospital/management/sql/hospital_objects.sql
- src/main/resources/hospital/management/sql/hospital_indexes_postgresql.sql
- src/main/resources/hospital/management/sql/hospital_seed_data.sql
- TESTING_REPORT.md

## 12. Acceptance Checklist
- [x] Relational vs NoSQL design comparison structure prepared
- [x] Scope constraints documented
- [x] Before/after benchmark matrix prepared
- [ ] Numeric benchmark values captured
- [ ] Query plan snapshots included
- [ ] Improvement percentages computed from measured data
