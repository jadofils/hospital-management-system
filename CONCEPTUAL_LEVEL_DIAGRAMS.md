# Conceptual Level Diagrams

This document provides conceptual-level views (business entities and relationships), independent from implementation details.

## 1. Core Clinical Conceptual Model

```mermaid
erDiagram
    DEPARTMENT ||--o{ DOCTOR : has
    DOCTOR ||--o{ APPOINTMENT : serves
    PATIENT ||--o{ APPOINTMENT : books

    APPOINTMENT ||--o| MEDICAL_RECORD : results_in
    APPOINTMENT ||--o{ VITAL_SIGN : captures
    PATIENT ||--o{ PATIENT_ALLERGY : has

    APPOINTMENT ||--o{ REFERRAL : may_generate
    DOCTOR ||--o{ REFERRAL : refers
```

## 2. Pharmacy and Lab Conceptual Model

```mermaid
erDiagram
    MEDICATION ||--o{ MEDICAL_INVENTORY : stocked_as
    APPOINTMENT ||--o{ PRESCRIPTION : may_issue
    PRESCRIPTION ||--o{ PRESCRIPTION_ITEM : contains
    MEDICATION ||--o{ PRESCRIPTION_ITEM : prescribed_as

    APPOINTMENT ||--o{ LAB_ORDER : requests
    DOCTOR ||--o{ LAB_ORDER : orders
    LAB_ORDER ||--o| LAB_RESULT : produces
```

## 3. Finance, Feedback, and Security Conceptual Model

```mermaid
erDiagram
    APPOINTMENT ||--o{ INVOICE : billed_by
    PATIENT ||--o{ INVOICE : billed_to

    PATIENT ||--o{ PATIENT_FEEDBACK : submits
    APPOINTMENT ||--o{ PATIENT_FEEDBACK : about

    USER ||--o{ USER_ROLE : assigned
    ROLE ||--o{ USER_ROLE : maps
    ROLE ||--o{ ROLE_PERMISSION : grants
    PERMISSION ||--o{ ROLE_PERMISSION : defines
```

## 4. Conceptual Notes

- A patient can have many appointments over time.
- An appointment can produce one medical record and multiple vitals/lab/prescription artifacts.
- Inventory and prescriptions are linked through medication entities.
- Billing and feedback are conceptually attached to care events (appointments) and patients.
- Access control follows role-permission mapping with user-role assignments.
