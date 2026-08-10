-- Root sample-data entrypoint for local/demo environments.
-- Run this after applying:
--   1) src/main/resources/hospital/management/sql/hospital_schema.sql
--   2) src/main/resources/hospital/management/sql/hospital_objects.sql

BEGIN;

-- Core domain sample data (patients, appointments, records, pharmacy, lab, invoices, feedback)
\i src/main/resources/hospital/management/sql/hospital_seed_data.sql

-- Optional RBAC sample data (roles, permissions, sample users)
\i src/main/resources/hospital/management/sql/hospital_rbac_seed_postgresql.sql

COMMIT;
