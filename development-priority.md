# Hospital Management System — Table Development Priority

This is the order to actually **build** things in — which table's CRUD screen/DAO to implement first, and why — as opposed to the raw SQL creation order (which is just "whatever satisfies foreign keys," documented at the bottom for reference).

Development order isn't the same as FK order: you build in an order that lets you *test something real* as early as possible, even though a few tables technically have no dependencies at all (like `medications`) but aren't useful to build first because nothing else is ready to connect to them yet.

---

## Phase 0 — Foundation (build first, nothing works without this)

| Table | Why first |
|---|---|
| `departments` | Simplest possible table — no dependencies, one field of real content (`name`). Good first JDBC CRUD screen to prove your Controller → Service → DAO wiring actually works end to end before anything harder. |
| `doctors` | Depends only on `departments`. Second CRUD screen — proves a foreign-key dropdown/lookup works in the UI. |
| `users`, `roles`, `permissions`, `user_roles`, `role_permissions` | Build the login/RBAC skeleton now, even in a rough form. Every other screen needs to check "is this user allowed to do this" — retrofitting access control after 20 screens exist is much more painful than gating from the start. You don't need every permission fleshed out yet, just: log in, know who's logged in, know their role. |

**You should be able to, by the end of this phase:** log in as a seeded user, and add/edit/list departments and doctors.

---

## Phase 1 — Core Master Data

| Table | Why now |
|---|---|
| `patients` | No dependencies at all. This is Epic 2.1's headline feature ("add, update, delete patient... data") — build it now while it's still simple, before appointments need patients to exist. |
| `doctor_schedules` | Depends only on `doctors`. Needs to exist before appointment booking can check availability — build the schedule screen before the booking screen that depends on it. |

**You should be able to, by the end of this phase:** manage patients and set up each doctor's weekly availability.

---

## Phase 2 — Scheduling (the operational core)

| Table | Why now |
|---|---|
| `appointments` | This is the hub of the whole schema — nearly every clinical table hangs off it. Needs `patients`, `doctors`, and ideally `doctor_schedules` already working so booking-conflict logic has something to check against. Build `sp_book_appointment` and the double-booking triggers here. |

**You should be able to, by the end of this phase:** book, reschedule, and cancel appointments, with conflict detection actually enforced.

---

## Phase 3 — Clinical Workflow (what happens during a visit)

| Table | Why now |
|---|---|
| `patient_allergies` | Depends only on `patients` — build this *before* prescriptions, since a real workflow checks allergies before prescribing. |
| `vital_signs` | Depends on `appointments`. Simple form, good next step. |
| `medical_records` | Depends on `appointments` (1:0..1). The diagnosis/notes screen — core of "doctor writes notes" from your original ask. |
| `referrals` | Depends on `appointments` + `doctors` (twice). Build after `medical_records` since referrals conceptually follow from a diagnosis. This is the "doctor sends me to another doctor" feature. |

**You should be able to, by the end of this phase:** run a full visit — vitals, allergy check, diagnosis/notes, and optionally refer to another doctor.

---

## Phase 4 — Pharmacy (prescribing and dispensing)

| Table | Why now |
|---|---|
| `medications` | No dependencies, but not useful until there's a visit to prescribe against — build it right before you need it, not earlier. |
| `medical_inventory` | Depends on `medications`. Batch/stock screen — build before prescriptions need to decrement it. |
| `prescriptions` | Depends on `appointments`. |
| `prescription_items` | Depends on `prescriptions` + `medications`/`medical_inventory`. This is where `sp_issue_prescription` and its `SAVEPOINT` logic gets built and tested — the most complex single piece of business logic in the whole system, so build it once everything it touches already exists and is stable. |

**You should be able to, by the end of this phase:** issue a full prescription, with stock actually decrementing and low-stock detection working.

---

## Phase 5 — Lab Workflow

| Table | Why now |
|---|---|
| `lab_orders` | Depends on `appointments` + `doctors`. Structurally simple; low priority because it's a smaller, more isolated feature than pharmacy. |
| `lab_results` | Depends on `lab_orders` (1:1). |

---

## Phase 6 — Billing & Feedback (can be built in parallel with Phase 5)

| Table | Why now |
|---|---|
| `invoices` | Depends on `appointments` + `patients`. Needs `fn_calculate_invoice_total`, which depends on prescriptions already existing (Phase 4) — build billing after pharmacy, not before. |
| `patient_feedback` | Depends on `patients`, optionally `appointments`. Genuinely independent of everything else feature-wise — safe to slot in whenever there's spare time, even earlier if you want an easy win. |

---

## Phase 7 — RBAC Refinement & Observability (ongoing, not a single sprint)

| Table | Why last (as a *dedicated* phase) |
|---|---|
| `audit_log` | Needs real actions happening elsewhere first to actually log anything meaningful — but the trigger (`trg_log_appointment_status`) should be wired in back in Phase 2, not bolted on at the end. |
| `user_sessions` | Login session tracking — technically usable from Phase 0, but the "active sessions" admin view is only interesting once there's more than one type of user actually logging in, i.e. once Phase 4-5 staff accounts exist. |
| `system_logs` | Pure technical logging — wire this in incrementally as you build each DAO (log errors as they occur), not as a standalone feature at the end. |

This phase is really "go back and tighten every earlier phase's permission checks now that all 25 tables and 100 permissions exist" — the granular `role_permissions` mapping only makes full sense once you know what every screen actually needs to check.

---

## Quick Reference — Build Order List

1. `departments`
2. `doctors`
3. `users` / `roles` / `permissions` / `user_roles` / `role_permissions` (basic skeleton)
4. `patients`
5. `doctor_schedules`
6. `appointments`
7. `patient_allergies`
8. `vital_signs`
9. `medical_records`
10. `referrals`
11. `medications`
12. `medical_inventory`
13. `prescriptions`
14. `prescription_items`
15. `lab_orders`
16. `lab_results`
17. `invoices`
18. `patient_feedback`
19. `audit_log` / `user_sessions` / `system_logs` (wired in throughout, formalized here)

---

## For Reference — Raw SQL Creation Order (dependency-only, not development priority)

This is the order tables must be *created* in for foreign keys to resolve (already reflected in `hospital_schema.sql`), which is not the same list as above — e.g. `medications` has zero dependencies and could technically be created second, but there's no development reason to build its screen that early.

```
departments → doctors → patients → appointments → medical_records → referrals →
patient_allergies → vital_signs → medications → medical_inventory → prescriptions →
prescription_items → lab_orders → lab_results → doctor_schedules → patient_feedback →
invoices → users → roles → permissions → user_roles → role_permissions →
audit_log → user_sessions → system_logs
```