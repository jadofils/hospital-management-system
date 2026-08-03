# Backend Structure — Folder Map & Interaction Diagram

Shows every folder in the backend, what lives in it, and how all the pieces
talk to each other at runtime.

---

## Full Folder Tree

```
src/main/java/hospital/management/
│
├── Main.java                          JavaFX entry point — starts app + daemon
│
├── enums/                             UI-side enums (PageRoute, BloodGroup, etc.)
├── pages/                             JavaFX page controllers (one per screen)
│   ├── AuthPageController
│   ├── DashboardController
│   ├── PatientsPageController
│   ├── AppointmentsPageController
│   ├── BillingPageController
│   └── RetentionSettingsController
├── pages/components/                  Reusable UI component controllers
│
└── backend/
    │
    ├── config/                        ── INFRASTRUCTURE ──────────────────────
    │   ├── AppConfig.java             App-wide constants
    │   ├── AppLogger.java             Logging wrapper
    │   ├── EnvConfig.java             Reads .env / system properties
    │   ├── MailConfig.java            SMTP settings
    │   ├── CloudinaryConfig.java      File upload settings
    │   ├── db/
    │   │   ├── DBConfig.java          Pool constants + credential delegation
    │   │   └── DBConnection.java      HikariCP pool, getConnection(), isHealthy()
    │   └── security/
    │       ├── EncryptionConfig.java  AES-256-GCM encrypt/decrypt
    │       ├── JwtConfig.java         JWE token generate/parse
    │       ├── PasswordConfig.java    BCrypt hash/verify
    │       ├── SessionManager.java    Static current-user state
    │       └── AccessControl.java     require(RoleName...) — throws ForbiddenException
    │
    ├── exceptions/                    ── ERROR TYPES ──────────────────────────
    │   ├── AppException               Base (RuntimeException)
    │   ├── AuthException              extends AppException
    │   │   ├── TokenExpiredException
    │   │   ├── UnauthorizedException
    │   │   └── ForbiddenException
    │   ├── ValidationException        carries field name
    │   ├── ResourceNotFoundException   carries type + id
    │   ├── EncryptionException
    │   ├── ConfigurationException
    │   ├── FileUploadException
    │   ├── DatabaseException          checked — extends Exception
    │   └── MailException              checked — extends Exception
    │
    ├── model/                         ── DOMAIN ENTITIES ──────────────────────
    │   ├── base/
    │   │   ├── Identifiable           interface: getId(), getEntityType()
    │   │   ├── Auditable              interface: getCreatedAt()
    │   │   ├── SoftDeletable          interface: getDeletedAt(), isDeleted(), markDeleted()
    │   │   ├── BaseEntity             abstract: id + timestamps + implements all 3
    │   │   ├── Person                 abstract extends BaseEntity: name + phone + email
    │   │   └── BaseLog                abstract: logId + userId + createdAt (no soft-delete)
    │   ├── user/                      User, Role, UserRole, RolePermission,
    │   │                              Permission, UserSession, AuditLog, SystemLog
    │   ├── patient/                   Patient, Appointment, MedicalRecord,
    │   │                              VitalSign, PatientAllergy, PatientFeedback
    │   ├── doctor/                    Doctor, Department, DoctorSchedule, Referral
    │   ├── lab/                       LabOrder, LabResult
    │   ├── pharmacy/                  Medication, MedicalInventory, Prescription, PrescriptionItem
    │   ├── finance/                   Invoice
    │   └── enums/                     Gender, RoleName, AppointmentStatus, LabOrderStatus,
    │                                  PaymentStatus, AllergySeverity, ScheduleDay,
    │                                  SystemLogLevel, PermissionAction, ReferralStatus
    │
    ├── utils/                         ── SHARED UTILITIES ─────────────────────
    │   ├── SanitizeUtils              maskForLog, stripControlChars, clean
    │   ├── ValidatorUtils             requireNonBlank, requireRange, isValidEmail…
    │   ├── filters/
    │   │   ├── EntityFilter<T>        @FunctionalInterface — in-memory predicate
    │   │   ├── FilterBuilder<T>       composes EntityFilters, applies to List<T>
    │   │   └── SqlFilterBuilder       builds parameterized WHERE clauses
    │   ├── pagination/
    │   │   ├── PageRequest            cursor + pageSize + direction
    │   │   ├── PageResult<T>          items + nextCursor + hasMore
    │   │   └── CursorPagination       encode/decode cursor, build SQL fragments
    │   ├── listeners/
    │   │   ├── AppEventType           enum of all publishable events
    │   │   ├── AppEvent               type + payload + occurredAt
    │   │   └── EventBus               subscribe/publish — delivers on FX thread
    │   └── pipes/
    │       ├── DataPipe<T,R>          @FunctionalInterface — one transformation step
    │       ├── PipelineRunner<I,O>    chains DataPipes, run() / runAll()
    │       └── AsyncJobRunner         thread pool + JavaFX Task for background work
    │
    ├── daemon/                        ── BACKGROUND CLEANUP ───────────────────
    │   ├── RetentionPolicy            holds all threshold values + DEFAULT_* constants
    │   ├── RetentionPolicyStore       load/save to ~/.hms/retention.properties
    │   ├── CleanupTask                interface: getName(), run(policy) → summary
    │   ├── UserInactivityCleaner      implements CleanupTask — deactivates idle users
    │   ├── DbLogCleaner               implements CleanupTask — hard-deletes old log rows
    │   ├── FileLogArchiver            implements CleanupTask — gzip + expire log files
    │   └── DatabaseCleanupDaemon      scheduler: start/stop/restart/runNow
    │
    ├── dto/                           ── DATA TRANSFER OBJECTS ────────────────
    │   ├── auth/                      LoginRequestDTO, LoginResponseDTO, UserDTO,
    │   │                              CreateUserDTO, UpdateUserDTO, RoleDTO,
    │   │                              CreateRoleDTO, PermissionDTO, UserSessionDTO
    │   ├── patient/                   PatientDTO, CreatePatientDTO, UpdatePatientDTO,
    │   │                              PatientSummaryDTO, VitalSignDTO, CreateVitalSignDTO,
    │   │                              PatientAllergyDTO, CreatePatientAllergyDTO,
    │   │                              PatientFeedbackDTO, CreatePatientFeedbackDTO
    │   ├── doctor/                    DepartmentDTO, CreateDepartmentDTO,
    │   │                              DoctorDTO, CreateDoctorDTO, DoctorSummaryDTO,
    │   │                              DoctorScheduleDTO, CreateDoctorScheduleDTO,
    │   │                              ReferralDTO, CreateReferralDTO
    │   ├── clinical/                  AppointmentDTO, CreateAppointmentDTO,
    │   │                              UpdateAppointmentDTO, AppointmentSummaryDTO,
    │   │                              MedicalRecordDTO, CreateMedicalRecordDTO
    │   ├── lab/                       LabOrderDTO, CreateLabOrderDTO,
    │   │                              LabResultDTO, CreateLabResultDTO
    │   ├── pharmacy/                  MedicationDTO, CreateMedicationDTO,
    │   │                              MedicalInventoryDTO, CreateMedicalInventoryDTO,
    │   │                              PrescriptionDTO, CreatePrescriptionDTO,
    │   │                              PrescriptionItemDTO, CreatePrescriptionItemDTO
    │   ├── finance/                   InvoiceDTO, CreateInvoiceDTO, InvoiceSummaryDTO
    │   └── log/                       AuditLogDTO, SystemLogDTO
    │
    ├── mapper/                        ── ENTITY ↔ DTO CONVERTERS ──────────────
    │   └── (22 mapper classes — one or two per entity)
    │       UserMapper, RoleMapper, UserSessionMapper,
    │       PatientMapper, VitalSignMapper, PatientAllergyMapper, PatientFeedbackMapper,
    │       DepartmentMapper, DoctorMapper, DoctorScheduleMapper, ReferralMapper,
    │       AppointmentMapper, MedicalRecordMapper,
    │       LabOrderMapper, LabResultMapper,
    │       MedicationMapper, MedicalInventoryMapper,
    │       PrescriptionMapper, PrescriptionItemMapper,
    │       InvoiceMapper, AuditLogMapper, SystemLogMapper
    │
    ├── dao/                           ── DATABASE ACCESS ───────────────────────
    │   ├── auth/                      UserDAO+Impl, RoleDAO+Impl,
    │   │                              UserRoleDAO+Impl, UserSessionDAO+Impl
    │   ├── department/                DepartmentDAO+Impl, DoctorDAO+Impl,
    │   │                              DoctorScheduleDAO+Impl, ReferralDAO+Impl
    │   ├── patient/                   PatientDAO+Impl, VitalSignDAO+Impl,
    │   │                              PatientAllergyDAO+Impl, PatientFeedbackDAO+Impl
    │   ├── clinical/                  AppointmentDAO+Impl, MedicalRecordDAO+Impl
    │   ├── lab/                       LabOrderDAO+Impl, LabResultDAO+Impl
    │   ├── pharmacy/                  MedicationDAO+Impl, MedicalInventoryDAO+Impl,
    │   │                              PrescriptionDAO+Impl, PrescriptionItemDAO+Impl
    │   ├── finance/                   InvoiceDAO+Impl
    │   └── log/                       AuditLogDAO+Impl, SystemLogDAO+Impl
    │
    └── service/                       ── BUSINESS LOGIC ────────────────────────
        ├── auth/                      AuthService+Impl, UserService+Impl, RoleService+Impl
        ├── department/                DepartmentService+Impl, DoctorService+Impl,
        │                              DoctorScheduleService+Impl, ReferralService+Impl
        ├── patient/                   PatientService+Impl, VitalSignService+Impl,
        │                              AllergyService+Impl, FeedbackService+Impl
        ├── clinical/                  AppointmentService+Impl, MedicalRecordService+Impl
        ├── lab/                       LabService+Impl
        ├── pharmacy/                  PharmacyService+Impl, PrescriptionService+Impl
        ├── finance/                   InvoiceService+Impl
        └── log/                       AuditService+Impl, SystemLogService+Impl
```

---

## Layer Interaction Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         JAVAFX UI LAYER                             │
│                                                                     │
│  AuthPageController   PatientsPageController   BillingPageController│
│  DashboardController  AppointmentsPageController                    │
│  RetentionSettingsController                                        │
└───────────────────────────────┬─────────────────────────────────────┘
                                │  calls with DTOs, receives DTOs
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        SERVICE LAYER                                │
│                                                                     │
│  auth/    AuthService   UserService   RoleService                   │
│  dept/    DepartmentService  DoctorService  DoctorScheduleService   │
│           ReferralService                                           │
│  patient/ PatientService  VitalSignService  AllergyService          │
│           FeedbackService                                           │
│  clinical/ AppointmentService  MedicalRecordService                 │
│  lab/     LabService                                                │
│  pharmacy/ PharmacyService  PrescriptionService                     │
│  finance/ InvoiceService                                            │
│  log/     AuditService  SystemLogService                            │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Uses:  Mapper.toEntity()  /  Mapper.toDTO()                │   │
│  │  Uses:  ValidatorUtils, SanitizeUtils                        │   │
│  │  Uses:  EncryptionConfig, JwtConfig, PasswordConfig          │   │
│  │  Uses:  AccessControl.require()                              │   │
│  │  Uses:  EventBus.publish()                                   │   │
│  │  Uses:  CursorPagination  (builds PageRequest/PageResult)    │   │
│  └──────────────────────────────────────────────────────────────┘   │
└───────────────────────────────┬─────────────────────────────────────┘
                                │  calls with entities, receives entities
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          DAO LAYER                                  │
│                                                                     │
│  auth/    UserDAO  RoleDAO  UserRoleDAO  UserSessionDAO             │
│  dept/    DepartmentDAO  DoctorDAO  DoctorScheduleDAO  ReferralDAO  │
│  patient/ PatientDAO  VitalSignDAO  PatientAllergyDAO               │
│           PatientFeedbackDAO                                        │
│  clinical/ AppointmentDAO  MedicalRecordDAO                         │
│  lab/     LabOrderDAO  LabResultDAO                                 │
│  pharmacy/ MedicationDAO  MedicalInventoryDAO  PrescriptionDAO      │
│           PrescriptionItemDAO                                        │
│  finance/ InvoiceDAO                                                │
│  log/     AuditLogDAO  SystemLogDAO                                 │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Uses:  DBConnection.getConnection()                         │   │
│  │  Uses:  SqlFilterBuilder (safe WHERE clauses)                │   │
│  │  Uses:  CursorPagination.whereClause() / orderClause()       │   │
│  └──────────────────────────────────────────────────────────────┘   │
└───────────────────────────────┬─────────────────────────────────────┘
                                │  JDBC PreparedStatement
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         PostgreSQL Database                         │
│                                                                     │
│  users  roles  user_roles  role_permissions  permissions            │
│  user_sessions                                                      │
│  patients  appointments  medical_records                            │
│  vital_signs  patient_allergies  patient_feedback                   │
│  departments  doctors  doctor_schedules  referrals                  │
│  lab_orders  lab_results                                            │
│  medications  medical_inventory  prescriptions  prescription_items  │
│  invoices                                                           │
│  audit_log  system_logs                                             │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Cross-Cutting Concern Diagram

These packages are used by multiple layers — they do not belong to any single layer.

```
                    ┌─────────────────────────────┐
                    │       model/                │
                    │  (entity classes)           │
                    │  used by: DAO + Service     │
                    └─────────────────────────────┘

                    ┌─────────────────────────────┐
                    │       dto/  +  mapper/      │
                    │  used by: Service + UI      │
                    └─────────────────────────────┘

┌──────────────┐   ┌─────────────────────────────┐   ┌──────────────┐
│   config/    │   │       utils/                │   │  exceptions/ │
│  security/   │   │  SanitizeUtils              │   │              │
│  db/         │   │  ValidatorUtils             │   │  thrown by:  │
│              │   │  filters/                   │   │  any layer   │
│  used by:    │   │  pagination/                │   │              │
│  Service     │   │  listeners/ (EventBus)      │   │  caught by:  │
│  DAO (db/)   │   │  pipes/                     │   │  UI / caller │
│  config uses │   │                             │   │              │
│  utils       │   │  used by: all layers        │   │              │
└──────────────┘   └─────────────────────────────┘   └──────────────┘

                    ┌─────────────────────────────┐
                    │       daemon/               │
                    │  DatabaseCleanupDaemon      │
                    │  started by: Main.java      │
                    │  configured by:             │
                    │    RetentionSettingsCtrl    │
                    │  uses: DAO (log tables)     │
                    │  uses: EventBus (publish)   │
                    └─────────────────────────────┘
```

---

## Domain Dependency Map

Shows which DAO domain depends on another (FK direction in DB).

```
auth ──────────────────────────────────────────── (no FK to other domains)
  │
  │  users.doctor_id → doctors
  ▼
department ────────────────────────────────────── (departments are root)
  │
  │  doctors.department_id → departments
  ▼
patient ───────────────────────────────────────── (patients are root)
  │
  │  appointments.patient_id  → patients
  │  appointments.doctor_id   → doctors
  ▼
clinical ──────────────────────────────────────── (depends on patient + department)
  │
  ├── lab/          lab_orders.appointment_id → appointments
  │                 lab_orders.doctor_id      → doctors
  │                 lab_results.lab_order_id  → lab_orders
  │
  ├── pharmacy/     prescriptions.appointment_id → appointments
  │                 prescription_items.medication_id → medications
  │                 medical_inventory.medication_id  → medications
  │
  └── finance/      invoices.appointment_id → appointments
                    invoices.patient_id     → patients

log ───────────────────────────────────────────── (no FK to business tables)
  audit_log.user_id  → users  (informational, not enforced as hard FK in cleanup)
  system_logs  (no user FK — may be written before login)
```

---

## Data Flow: Login Request End-to-End

```
1. User types username + password → AuthPageController

2. AuthPageController creates:
       LoginRequestDTO { username, password }
   calls:
       AuthService.login(dto)

3. AuthServiceImpl.login():
   a. ValidatorUtils.requireNonBlank(username, "username")
   b. UserDAO.findByUsername(username) → Optional<User>
   c. if empty → throw UnauthorizedException
   d. PasswordConfig.verify(password, user.passwordHash)
   e. if false  → throw UnauthorizedException
   f. if !user.isActive → throw ForbiddenException
   g. token = JwtConfig.generateToken(userId, username, role)
   h. session = new UserSession(...)
   i. UserSessionDAO.save(session)
   j. SessionManager.login(token)
   k. EventBus.publish(USER_LOGGED_IN, userId)
   l. return new LoginResponseDTO(token, userId, username, role)

4. AuthPageController receives LoginResponseDTO
   → navigates to dashboard.fxml

5. EventBus delivers USER_LOGGED_IN to any listener
   (e.g. AuditService records the login event)
```

---

## Data Flow: Booking an Appointment End-to-End

```
1. AppointmentsPageController collects form fields:
       CreateAppointmentDTO { patientId, doctorId, appointmentDate, reason }
   calls:
       AppointmentService.book(dto)

2. AppointmentServiceImpl.book():
   a. AccessControl.require(DOCTOR, ADMIN)
   b. validate appointmentDate is in the future
   c. DoctorScheduleDAO.findByDoctorId(doctorId)
      → check doctor is available on that day/time
   d. entity = AppointmentMapper.toEntity(dto)   ← sets status="scheduled"
   e. saved  = AppointmentDAO.save(entity)
   f. EventBus.publish(APPOINTMENT_CREATED, saved)
   g. AuditService.record(userId, "INSERT", "appointments", saved.getId())
   h. return AppointmentMapper.toDTO(saved)

3. Controller receives AppointmentDTO
   → shows confirmation toast via ToastController
```

---

## Data Flow: Daemon Cleanup Cycle

```
Main.start()
  └─► DatabaseCleanupDaemon.start()
          └─► schedules runCycle() every N hours

runCycle():
  1. RetentionPolicyStore.load() → RetentionPolicy
  2. EventBus.publish(DATA_CLEANING_STARTED, taskCount)

  3. UserInactivityCleaner.run(policy)
     └─► UPDATE users SET is_active=false WHERE ...
         returns "Deactivated X users"

  4. DbLogCleaner.run(policy)
     └─► DELETE FROM system_logs WHERE created_at < NOW() - interval
         DELETE FROM audit_log  WHERE created_at < NOW() - interval
         (single transaction, rolls back on error)
         returns "Deleted X system log rows, Y audit log rows"

  5. FileLogArchiver.run(policy)
     └─► scan ~/.hms/logs/ for .log files > maxMB → gzip them
         scan .log.gz files > retention days → delete them
         returns "Archived X files, deleted Y archives"

  6. EventBus.publish(DATA_CLEANING_COMPLETED, summaries)
     └─► RetentionSettingsController.onCleaningCompleted(summaries)
             → appends to dark log TextArea
             → re-enables Run Now button
             → updates Last Run label
```

---

## Summary: What Each Package Is Responsible For

| Package | Responsibility | Talks to |
|---|---|---|
| `pages/` | Display, user input, navigation | Service layer (via DTOs) |
| `config/security/` | Encryption, JWT, sessions, access rules | Used by Service |
| `config/db/` | Connection pool | Used by DAO |
| `model/` | Entity shape (DB row as Java object) | DAO + Mapper |
| `dto/` | Data shape for UI communication | Controller + Service + Mapper |
| `mapper/` | Convert entity ↔ DTO | Service |
| `utils/` | Shared tools (validate, paginate, filter, events, pipes) | All layers |
| `exceptions/` | Named error types | Thrown anywhere, caught by controller |
| `dao/` | SQL only — reads/writes database | Database |
| `service/` | Business rules — orchestrates DAO + Mapper | DAO + Mapper + Utils |
| `daemon/` | Scheduled background cleanup | DAO (log tables) + EventBus |