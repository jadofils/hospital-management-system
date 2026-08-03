# Performance Report Template: Relational vs NoSQL for Unstructured Clinical Data

## 1. Report Metadata
- Project: Hospital/Healthcare Management System
- Author(s):
- Date:
- Environment:
- PostgreSQL version:
- MongoDB version:
- Dataset version/tag:

## 2. Objective
This report compares relational and NoSQL approaches for unstructured/semi-structured healthcare data (patient notes, medical logs, notification payloads), and documents performance improvements from optimization and indexing.

## 3. Scope and Boundaries
- PostgreSQL remains the source of truth for transactional entities (patients, appointments, billing, prescriptions, inventory, users/roles).
- MongoDB is used only for unstructured/semi-structured records.
- Comparison target entities:
  - Patient notes
  - Medical logs
  - Notification payloads

## 4. Data Models Compared

### 4.1 PostgreSQL Model (Relational)
- Table(s):
- Key columns:
- JSON/JSONB columns (if used):
- Constraints:

### 4.2 MongoDB Model (NoSQL)
- Database/collection(s):
- Document schema sample:
- Embedded vs referenced choices:
- Validation rules (if any):

## 5. Benchmark Methodology
- Workload type: read-heavy / write-heavy / mixed
- Number of records tested:
- Batch sizes:
- Concurrency level:
- Warm-up strategy:
- Number of measured runs:
- How outliers were handled:
- Hardware/runtime notes:

## 6. Test Operations
Define equivalent operations for both stores.

1. Insert single unstructured note/log
2. Insert batch records
3. Fetch by patientId + time range
4. Fetch by free-text keyword
5. Fetch most recent N records
6. Update note/log status or tag

## 7. Baseline (Before Optimization)

### 7.1 PostgreSQL Baseline
- Query plan evidence (EXPLAIN ANALYZE):
- Average latency (ms):
- P95 latency (ms):
- Throughput (ops/sec):

### 7.2 MongoDB Baseline
- Query plan evidence (explain):
- Average latency (ms):
- P95 latency (ms):
- Throughput (ops/sec):

## 8. Optimizations Applied

### 8.1 PostgreSQL
- Indexes created (example):
  - CREATE INDEX idx_notes_patient_time ON patient_notes(patient_id, created_at DESC);
  - CREATE INDEX idx_logs_type_time ON medical_logs(log_type, created_at DESC);
- Query rewrites/caching:

### 8.2 MongoDB
- Indexes created (example):
  - db.notifications.createIndex({ recipients: 1, createdAt: -1 })
  - db.medical_logs.createIndex({ patientId: 1, createdAt: -1 })
  - db.patient_notes.createIndex({ text: "text" })
- Document shape adjustments:

## 9. Results (After Optimization)

| Operation | Store | Avg Latency Before (ms) | Avg Latency After (ms) | P95 Before (ms) | P95 After (ms) | Throughput Before (ops/s) | Throughput After (ops/s) | Improvement % |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| Insert single | PostgreSQL |  |  |  |  |  |  |  |
| Insert single | MongoDB |  |  |  |  |  |  |  |
| Insert batch | PostgreSQL |  |  |  |  |  |  |  |
| Insert batch | MongoDB |  |  |  |  |  |  |  |
| Fetch by patient + time | PostgreSQL |  |  |  |  |  |  |  |
| Fetch by patient + time | MongoDB |  |  |  |  |  |  |  |
| Keyword search | PostgreSQL |  |  |  |  |  |  |  |
| Keyword search | MongoDB |  |  |  |  |  |  |  |
| Recent N records | PostgreSQL |  |  |  |  |  |  |  |
| Recent N records | MongoDB |  |  |  |  |  |  |  |

Improvement formula:
- Latency improvement % = ((Before - After) / Before) * 100
- Throughput improvement % = ((After - Before) / Before) * 100

## 10. Analysis and Interpretation
- Which store performed better per operation and why:
- Effect of indexing on each operation:
- Trade-offs (consistency, schema evolution, query complexity, operational overhead):
- Suitability of each store for unstructured healthcare data:

## 11. Conclusion and Recommendation
- Final recommendation for unstructured patient notes/logs:
- Cases where PostgreSQL should still be preferred:
- Cases where MongoDB should be preferred:

## 12. Evidence Appendix
- SQL scripts used:
- Mongo shell scripts used:
- Raw timing outputs:
- Query plan screenshots/logs:
- Reproducibility notes:

## 13. Acceptance Checklist
- [ ] Relational vs NoSQL design comparison completed
- [ ] Unstructured-data scope limited to notes/logs/payloads
- [ ] Before/after indexing benchmarks included
- [ ] Query plans included (PostgreSQL EXPLAIN ANALYZE and MongoDB explain)
- [ ] Improvement percentages computed and justified
- [ ] Recommendation aligns with project constraints
