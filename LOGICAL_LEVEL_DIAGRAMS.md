# Logical Level Diagrams

This document describes the logical data model for the Hospital Management System, including entities, key attributes, and normalized relationships.

## 1. Clinical and Patient Care Logical Model

```mermaid
erDiagram
    DEPARTMENT ||--o{ DOCTOR : contains
    PATIENT ||--o{ APPOINTMENT : schedules
    DOCTOR ||--o{ APPOINTMENT : attends

    APPOINTMENT ||--o| MEDICAL_RECORD : has
    APPOINTMENT ||--o{ VITAL_SIGN : captures
    PATIENT ||--o{ PATIENT_ALLERGY : owns

    APPOINTMENT ||--o{ REFERRAL : creates
    DOCTOR ||--o{ REFERRAL : issues

    PATIENT {
      uuid patient_id PK
      string first_name
      string last_name
      date dob
      string gender
      string phone
      string email
      string address
    }

    APPOINTMENT {
      uuid appointment_id PK
      uuid patient_id FK
      uuid doctor_id FK
      datetime appointment_date
      string status
      string reason
    }
```

## 2. Pharmacy and Lab Logical Model

```mermaid
erDiagram
    MEDICATION ||--o{ MEDICAL_INVENTORY : stocked_as
    APPOINTMENT ||--o{ PRESCRIPTION : issues
    PRESCRIPTION ||--o{ PRESCRIPTION_ITEM : contains
    MEDICATION ||--o{ PRESCRIPTION_ITEM : referenced_by

    APPOINTMENT ||--o{ LAB_ORDER : requests
    LAB_ORDER ||--o| LAB_RESULT : resolves

    MEDICATION {
      uuid medication_id PK
      string name
      string generic_name
      string form
      decimal unit_price
    }

    MEDICAL_INVENTORY {
      uuid inventory_id PK
      uuid medication_id FK
      string batch_number
      date expiry_date
      int quantity_in_stock
      int reorder_level
      string supplier
    }
```

## 3. Billing, Feedback, and Access Control Logical Model

```mermaid
erDiagram
    PATIENT ||--o{ INVOICE : receives
    APPOINTMENT ||--o{ INVOICE : generates

    PATIENT ||--o{ PATIENT_FEEDBACK : submits
    APPOINTMENT ||--o{ PATIENT_FEEDBACK : references

    USER ||--o{ USER_ROLE : mapped_to
    ROLE ||--o{ USER_ROLE : mapped_to
    ROLE ||--o{ ROLE_PERMISSION : mapped_to
    PERMISSION ||--o{ ROLE_PERMISSION : mapped_to

    INVOICE {
      uuid invoice_id PK
      uuid appointment_id FK
      uuid patient_id FK
      decimal total_amount
      string payment_status
      datetime issued_at
    }

    PERMISSION {
      uuid permission_id PK
      string resource
      string action
    }
```

## 4. Logical Design Notes

- The logical model follows 3NF-oriented separation of concerns.
- Many-to-many role assignment and permission assignment are resolved through bridge tables.
- Medical and operational workflows are linked through appointment_id for traceability.
- UUID keys are used across the model for consistency and distributed-safe identifiers.
