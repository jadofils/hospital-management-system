# Backend Data Layer — Learning Report

Covers everything built in the `dao/`, `service/`, `dto/`, and `mapper/` packages.
Read this after `BACKEND_ARCHITECTURE.md` and `BACKEND_UTILITIES.md`.

---

## The Problem This Layer Solves

The models (`Patient`, `Doctor`, `Invoice`, …) represent what is stored in the database.
The UI controllers need to display, create, and update that data — but they should never
reach directly into the database. Mixing database code into JavaFX controllers causes:

- Hard-to-test code (you can't test UI logic without a real DB)
- Duplicated SQL across files
- No single place to put business rules (e.g. "a user must have at least one role")

The data layer inserts four kinds of objects between the controller and the database:

```
UI Controller
    │  passes/receives DTOs
    ▼
Service           ← business rules live here
    │  calls
    ▼
DAO               ← SQL lives here
    │  reads/writes
    ▼
Database
```

Mappers translate between the Service's world (entities + DTOs) without either layer
knowing how the other works internally.

---

## 1 — DTOs (Data Transfer Objects)

### What they are

A DTO is a plain Java class that carries only the data needed for one specific operation.
It has no behaviour — just fields, a constructor, getters, setters, and `toString`.

### Why not just use the model directly?

| Problem | Example |
|---|---|
| Models expose internal fields | `User.passwordHash` must never reach the UI |
| Models carry DB timestamps | `createdAt`, `updatedAt` are irrelevant when creating a new record |
| Same model, different shapes | A patient list needs only name+phone; a patient detail page needs everything |

### Naming convention used

| Suffix | Meaning | Example |
|---|---|---|
| `DTO` | Full read response | `PatientDTO` — everything about a patient |
| `CreateXxxDTO` | Data needed to create a new record | `CreatePatientDTO` — no ID, no timestamps |
| `UpdateXxxDTO` | Partial update fields only | `UpdatePatientDTO` — patientId + mutable fields |
| `XxxSummaryDTO` | Lightweight list/card view | `PatientSummaryDTO` — id, name, phone, email only |

### Package layout

```
dto/
├── auth/          LoginRequestDTO, LoginResponseDTO,
│                  UserDTO, CreateUserDTO, UpdateUserDTO,
│                  RoleDTO, CreateRoleDTO, PermissionDTO, UserSessionDTO
│
├── patient/       PatientDTO, CreatePatientDTO, UpdatePatientDTO, PatientSummaryDTO,
│                  VitalSignDTO, CreateVitalSignDTO,
│                  PatientAllergyDTO, CreatePatientAllergyDTO,
│                  PatientFeedbackDTO, CreatePatientFeedbackDTO
│
├── doctor/        DepartmentDTO, CreateDepartmentDTO,
│                  DoctorDTO, CreateDoctorDTO, DoctorSummaryDTO,
│                  DoctorScheduleDTO, CreateDoctorScheduleDTO,
│                  ReferralDTO, CreateReferralDTO
│
├── clinical/      AppointmentDTO, CreateAppointmentDTO, UpdateAppointmentDTO,
│                  AppointmentSummaryDTO,
│                  MedicalRecordDTO, CreateMedicalRecordDTO
│
├── lab/           LabOrderDTO, CreateLabOrderDTO,
│                  LabResultDTO, CreateLabResultDTO
│
├── pharmacy/      MedicationDTO, CreateMedicationDTO,
│                  MedicalInventoryDTO, CreateMedicalInventoryDTO,
│                  PrescriptionDTO, CreatePrescriptionDTO,
│                  PrescriptionItemDTO, CreatePrescriptionItemDTO
│
├── finance/       InvoiceDTO, CreateInvoiceDTO, InvoiceSummaryDTO
│
└── log/           AuditLogDTO, SystemLogDTO
```

### Key design decisions

**LoginRequestDTO** contains only `username` and `password`.
The controller passes it to `AuthService.login()`, which validates credentials and
returns a `LoginResponseDTO` with the JWT token. The controller never sees the raw User entity.

**PrescriptionDTO** has a `List<PrescriptionItemDTO> items` field.
This allows the whole prescription (header + all line items) to be returned in one object.
The service loads items separately and assembles the DTO.

**InvoiceSummaryDTO** contains `patientName` (a joined string), not `patientId`.
This is the right shape for a billing list table — the controller gets exactly what it
needs to render a row without making a second request.

---

## 2 — Mappers

### What they are

A Mapper is a utility class with only `static` methods.
It converts between an entity and a DTO (or vice versa) so no other class has to know
how both sides are structured.

```java
// entity → DTO (reading from DB)
PatientDTO dto = PatientMapper.toDTO(patient);

// create DTO → entity (writing to DB)
Patient patient = PatientMapper.toEntity(createDTO);
```

### Why static methods (not instances)?

Mappers hold no state. They are pure functions. Making them static:
- Removes the need to inject them
- Makes them easier to call from anywhere
- Makes it obvious they cannot fail or have side effects

### Package layout

```
mapper/
├── UserMapper            User ↔ UserDTO / CreateUserDTO
├── RoleMapper            Role ↔ RoleDTO / CreateRoleDTO
├── UserSessionMapper     UserSession ↔ UserSessionDTO
├── PatientMapper         Patient ↔ PatientDTO / CreatePatientDTO / PatientSummaryDTO
├── VitalSignMapper       VitalSign ↔ VitalSignDTO / CreateVitalSignDTO
├── PatientAllergyMapper  PatientAllergy ↔ PatientAllergyDTO / CreatePatientAllergyDTO
├── PatientFeedbackMapper PatientFeedback ↔ PatientFeedbackDTO / CreatePatientFeedbackDTO
├── DepartmentMapper      Department ↔ DepartmentDTO / CreateDepartmentDTO
├── DoctorMapper          Doctor ↔ DoctorDTO / CreateDoctorDTO / DoctorSummaryDTO
├── DoctorScheduleMapper  DoctorSchedule ↔ DoctorScheduleDTO / CreateDoctorScheduleDTO
├── ReferralMapper        Referral ↔ ReferralDTO / CreateReferralDTO
├── AppointmentMapper     Appointment ↔ AppointmentDTO / CreateAppointmentDTO / AppointmentSummaryDTO
├── MedicalRecordMapper   MedicalRecord ↔ MedicalRecordDTO / CreateMedicalRecordDTO
├── LabOrderMapper        LabOrder ↔ LabOrderDTO / CreateLabOrderDTO
├── LabResultMapper       LabResult ↔ LabResultDTO / CreateLabResultDTO
├── MedicationMapper      Medication ↔ MedicationDTO / CreateMedicationDTO
├── MedicalInventoryMapper MedicalInventory ↔ MedicalInventoryDTO / CreateMedicalInventoryDTO
├── PrescriptionMapper    Prescription ↔ PrescriptionDTO / CreatePrescriptionDTO
├── PrescriptionItemMapper PrescriptionItem ↔ PrescriptionItemDTO / CreatePrescriptionItemDTO
├── InvoiceMapper         Invoice ↔ InvoiceDTO / CreateInvoiceDTO / InvoiceSummaryDTO
├── AuditLogMapper        AuditLog ↔ AuditLogDTO
└── SystemLogMapper       SystemLog ↔ SystemLogDTO
```

### Key design decisions

**DoctorMapper.toSummaryDTO** takes a `Doctor` and a `departmentName` String.
The mapper can't make a DB call, so the service resolves the department name first,
then passes it in. The mapper only does field assignment.

**PrescriptionMapper.toDTO** sets `items = null`.
Items are loaded by a separate DAO query and attached by the service layer.
The mapper documents this explicitly with a comment, so future developers know
it is intentional, not a bug.

**ReferralMapper.toEntity** hard-codes `status = "pending"`.
A new referral always starts pending — this business rule belongs in the mapper
because it is always true at creation time, not a runtime decision.

---

## 3 — DAOs (Data Access Objects)

### What they are

A DAO is the only object that is allowed to speak SQL.
Every DAO follows the same two-file pattern:

| File | Role |
|---|---|
| `XxxDAO` (interface) | Declares what operations exist |
| `XxxDAOImpl` (class) | Will contain the actual JDBC/SQL implementation |

Having an interface means services depend on the interface, not the implementation.
If you later swap PostgreSQL for another database, you write a new Impl — the Service
and everything above it stays unchanged.

### Package layout (grouped by table dependency)

```
dao/
├── auth/          Users and identity — no FK dependencies on other business tables
│   ├── UserDAO / UserDAOImpl
│   ├── RoleDAO / RoleDAOImpl
│   ├── UserRoleDAO / UserRoleDAOImpl    (junction: users ↔ roles)
│   └── UserSessionDAO / UserSessionDAOImpl
│
├── department/    Doctors depend on departments; schedules/referrals depend on doctors
│   ├── DepartmentDAO / DepartmentDAOImpl
│   ├── DoctorDAO / DoctorDAOImpl
│   ├── DoctorScheduleDAO / DoctorScheduleDAOImpl
│   └── ReferralDAO / ReferralDAOImpl
│
├── patient/       Patients are independent; vitals/allergies/feedback FK to patient
│   ├── PatientDAO / PatientDAOImpl
│   ├── VitalSignDAO / VitalSignDAOImpl
│   ├── PatientAllergyDAO / PatientAllergyDAOImpl
│   └── PatientFeedbackDAO / PatientFeedbackDAOImpl
│
├── clinical/      Appointments FK to patient + doctor; medical records FK to appointment
│   ├── AppointmentDAO / AppointmentDAOImpl
│   └── MedicalRecordDAO / MedicalRecordDAOImpl
│
├── lab/           Lab orders FK to appointment + doctor; results FK to order
│   ├── LabOrderDAO / LabOrderDAOImpl
│   └── LabResultDAO / LabResultDAOImpl
│
├── pharmacy/      Medications are base; inventory + prescription items FK to medication
│   ├── MedicationDAO / MedicationDAOImpl
│   ├── MedicalInventoryDAO / MedicalInventoryDAOImpl
│   ├── PrescriptionDAO / PrescriptionDAOImpl
│   └── PrescriptionItemDAO / PrescriptionItemDAOImpl
│
├── finance/       Invoices FK to appointment + patient
│   └── InvoiceDAO / InvoiceDAOImpl
│
└── log/           Log tables have no deleted_at and no FK to business tables
    ├── AuditLogDAO / AuditLogDAOImpl
    └── SystemLogDAO / SystemLogDAOImpl
```

### Why grouped this way?

The grouping mirrors the FK dependency tree in the database.
When you implement a DAO, you know what other tables it touches and what must exist first.
For example, you cannot implement `AppointmentDAOImpl` without understanding both the
`patients` and `doctors` tables — they are in the same conceptual neighbourhood.

### Standard method set per DAO

| Method | Returns | Purpose |
|---|---|---|
| `save(entity)` | entity with generated ID | INSERT |
| `findById(id)` | `Optional<Entity>` | SELECT by PK |
| `findAll(PageRequest)` | `PageResult<Entity>` | Paginated SELECT |
| `update(entity)` | updated entity | UPDATE |
| `softDelete(id)` | void | Sets `deleted_at = NOW()` |

Specialised methods are added per DAO as needed:
`UserDAO.findByUsername`, `PatientDAO.search`, `InvoiceDAO.findByPatientId`, etc.

### Soft delete vs hard delete

All business tables use soft delete (`deleted_at IS NULL`).
The log tables (`AuditLogDAO`, `SystemLogDAO`) use hard delete via
`deleteOlderThanDays(int days)` because logs are append-only and old ones
are genuinely discarded by the cleanup daemon.

---

## 4 — Services

### What they are

A Service holds business logic — the rules of the application that are neither
"display something" nor "run a SQL query".

Examples of business logic:
- Hashing the password before saving a new user
- Checking that a referred-to doctor is not the same as the referring doctor
- Deducting stock when a prescription is issued
- Ensuring an appointment cannot be booked in the past

Like DAOs, services follow the interface + implementation pattern:

| File | Role |
|---|---|
| `XxxService` (interface) | Declares what the service can do |
| `XxxServiceImpl` (class) | Will contain the actual business logic |

### Package layout (grouped by domain)

```
service/
├── auth/
│   ├── AuthService / AuthServiceImpl      login, logout, changePassword
│   ├── UserService / UserServiceImpl      CRUD for user accounts
│   └── RoleService / RoleServiceImpl      manage roles, assign to users
│
├── department/
│   ├── DepartmentService / DepartmentServiceImpl
│   ├── DoctorService / DoctorServiceImpl
│   ├── DoctorScheduleService / DoctorScheduleServiceImpl
│   └── ReferralService / ReferralServiceImpl
│
├── patient/
│   ├── PatientService / PatientServiceImpl
│   ├── VitalSignService / VitalSignServiceImpl
│   ├── AllergyService / AllergyServiceImpl
│   └── FeedbackService / FeedbackServiceImpl
│
├── clinical/
│   ├── AppointmentService / AppointmentServiceImpl
│   └── MedicalRecordService / MedicalRecordServiceImpl
│
├── lab/
│   └── LabService / LabServiceImpl          orders + results in one service
│
├── pharmacy/
│   ├── PharmacyService / PharmacyServiceImpl    medications + inventory
│   └── PrescriptionService / PrescriptionServiceImpl
│
├── finance/
│   └── InvoiceService / InvoiceServiceImpl
│
└── log/
    ├── AuditService / AuditServiceImpl
    └── SystemLogService / SystemLogServiceImpl
```

### Constructor injection

Every ServiceImpl receives its DAO dependencies through the constructor:

```java
public class DoctorServiceImpl implements DoctorService {

    private final DoctorDAO     doctorDAO;
    private final DepartmentDAO departmentDAO;

    public DoctorServiceImpl(DoctorDAO doctorDAO, DepartmentDAO departmentDAO) {
        this.doctorDAO     = doctorDAO;
        this.departmentDAO = departmentDAO;
    }
    ...
}
```

This means:
- The service cannot be used without its dependencies (no null-pointer surprises)
- In tests, you can pass a fake DAO instead of a real one
- The coupling is explicit and visible in one place

### Some services depend on more than one DAO

| Service | DAOs it uses | Reason |
|---|---|---|
| `AuthServiceImpl` | UserDAO, UserSessionDAO | Login creates a session; logout deactivates it |
| `DoctorServiceImpl` | DoctorDAO, DepartmentDAO | `toSummaryDTO` needs the department name |
| `LabServiceImpl` | LabOrderDAO, LabResultDAO | Orders and results are tightly coupled |
| `PrescriptionServiceImpl` | PrescriptionDAO, PrescriptionItemDAO | A prescription header + its line items |
| `RoleServiceImpl` | RoleDAO, UserRoleDAO | Assigning/revoking roles touches the junction table |

---

## 5 — How the Four Pieces Work Together

Here is a concrete end-to-end example: booking an appointment.

```
1. UI Controller receives form input
   ──────────────────────────────────────────────────────────
   CreateAppointmentDTO dto = new CreateAppointmentDTO(
       patientId, doctorId, selectedDate, reason
   );

2. Controller calls Service
   ──────────────────────────────────────────────────────────
   AppointmentDTO result = appointmentService.book(dto);

3. AppointmentServiceImpl.book() — business logic
   ──────────────────────────────────────────────────────────
   a) Validate: appointmentDate must be in the future
   b) Check: doctor is available at that time (calls DoctorScheduleDAO)
   c) Convert: Appointment entity = AppointmentMapper.toEntity(dto)
   d) Save:    Appointment saved = appointmentDAO.save(entity)
   e) Publish: EventBus.publish(APPOINTMENT_CREATED, saved)
   f) Convert back: return AppointmentMapper.toDTO(saved)

4. Controller receives AppointmentDTO
   ──────────────────────────────────────────────────────────
   Displays confirmation to user.
   Never saw the Appointment entity or any SQL.
```

The flow is always:

```
DTO in  →  Service  →  Mapper.toEntity  →  DAO.save  →  DB
DB      →  DAO.find  →  Mapper.toDTO  →  Service  →  DTO out
```

---

## 6 — What Is Still a Stub

Every DAO method and every Service method throws:
```java
throw new UnsupportedOperationException("Not implemented yet");
```

This means the code compiles and the structure is clear, but nothing touches the
database yet. The next phase is to implement them one DAO at a time:

1. Implement `UserDAOImpl` — write the JDBC PreparedStatement, map ResultSet → User
2. Implement `AuthServiceImpl.login()` — call UserDAO, verify password with PasswordConfig,
   generate token with JwtConfig, save session with UserSessionDAO
3. Wire the controller to `AuthService` and remove the TODO

Work outward from the tables with no FKs (`auth`, `patient`, `department`) before moving
to tables that depend on them (`clinical`, `lab`, `pharmacy`, `finance`).