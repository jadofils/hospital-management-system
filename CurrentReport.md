Research Report: Wiring JavaFX Table Pages to Backend Service Layer
Summary of key structural facts (read first)
PaginatedTableController<T>.setItems(List<T> items) (src/main/java/hospital/management/pages/components/PaginatedTableController.java) does not call any service itself. It just takes a plain List<T>, stores it in an internal ObservableList, and does its own client-side slicing into pages of ROWS_PER_PAGE = 10 via the JavaFX Pagination control (renderPage() on pagination.currentPageIndexProperty() changes). So pagination is purely a table-controller-internal display concern — page controllers should fetch the full/complete list of entities and hand it to setItems(List), not manage cursor pages themselves.
PageResult<T> (backend/utils/pagination/PageResult.java) has no "get everything" method — no unlimited/no-pagination findAll() variant exists on any service. getItems() only returns the current page's items.
CursorPagination.firstPage(int size) exists (backend/utils/pagination/CursorPagination.java line 48): public static PageRequest firstPage(int size) — returns a PageRequest(null, size, DESC). Since a page controller wants "all" rows for PaginatedTableController, the practical approach is to call service.findAll(CursorPagination.firstPage(LARGE_NUMBER)) (e.g. Integer.MAX_VALUE or a large constant) and pass result.getItems() to setItems(...), or loop pages via hasMore()/getNextCursor() and concatenate. There is no built-in "all" helper — this is a real gap to design around in the implementation plan.
Mapper direction pattern (near-universal): every mapper in backend/mapper/** provides toDTO(Entity) -> DTO and toEntity(CreateXDTO) -> Entity. None provide DTO -> Entity for the full DTO (only from the narrower Create-DTO), except AuditLogMapper.toEntity(AuditLogDTO) and SystemLogMapper.toEntity(SystemLogDTO) which take the full DTO directly (because those two have no Create*DTO type). This means: for list/read paths, findAll() returns PageResult<XDTO>, but PaginatedTableController/TableController subclasses expect the plain Entity/Model class — there is no ready-made mapper method to go DTO→full-Entity for populating the table with complete entity state (id, timestamps, etc.). The implementation plan must either (a) add new fromDTO/toEntity(DTO) overloads to each mapper, or (b) construct the Entity manually field-by-field in the page controller/an adapter.
Per-service breakdown
1. PatientService
File: backend/service/patient/interfaces/PatientService.java
Methods: create(CreatePatientDTO) : PatientDTO; findById(String) : PatientDTO; findAll(PageRequest) : PageResult<PatientDTO>; search(String query, PageRequest) : PageResult<PatientSummaryDTO>; update(UpdatePatientDTO) : PatientDTO; delete(String).
findAll DTO: PatientDTO (backend/dto/patient/PatientDTO.java), paginated via PageRequest/PageResult.
Mapper: backend/mapper/patient/PatientMapper.java — toDTO(Patient), toSummaryDTO(Patient), toEntity(CreatePatientDTO). Entity→DTO and CreateDTO→Entity only; no DTO→Entity.
TableController expects: Patient entity (backend/model/patient/Patient) — confirmed via PatientTableController.java import and PatientsPageController.java: private final List<Patient> patients = new ArrayList<>(); ... patientTableController.setItems(patients);
Constructor pattern (existing usage, e.g. EntityLookupService.java, InvoicePageController.java, AppointmentsPageController.java): new PatientServiceImpl(new PatientDAOImpl()) — single-DAO constructor: PatientServiceImpl(PatientDAO patientDAO).
2. DoctorService
File: backend/service/department/interfaces/DoctorService.java
Methods: create(CreateDoctorDTO) : DoctorDTO; findById(String) : DoctorDTO; findAll(PageRequest) : PageResult<DoctorDTO>; findByDepartment(String) : List<DoctorSummaryDTO>; update(String id, CreateDoctorDTO) : DoctorDTO; delete(String).
findAll DTO: DoctorDTO (backend/dto/doctor/DoctorDTO.java).
Mapper: backend/mapper/doctor/DoctorMapper.java — toDTO(Doctor), toSummaryDTO(Doctor, departmentName), toEntity(CreateDoctorDTO).
TableController expects: Doctor entity (backend/model/doctor/Doctor) — confirmed via DoctorTableController.java import.
Constructor pattern: new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl()) (multi-DAO, seen in AppointmentsPageController, ReferralsController, LabOrdersController, EntityLookupService) — DoctorServiceImpl(DoctorDAO doctorDAO, DepartmentDAO departmentDAO).
3. AppointmentService
File: backend/service/clinical/interfaces/AppointmentService.java
Methods: book(CreateAppointmentDTO) : AppointmentDTO; findById(String) : AppointmentDTO; findAll(PageRequest) : PageResult<AppointmentSummaryDTO> (note: findAll returns the Summary DTO, not AppointmentDTO); findByPatient(String) : List<AppointmentDTO>; findByDoctor(String) : List<AppointmentDTO>; update(UpdateAppointmentDTO) : AppointmentDTO; cancel(String).
findAll DTO: AppointmentSummaryDTO (backend/dto/clinical/AppointmentSummaryDTO.java); other reads return full AppointmentDTO (backend/dto/clinical/AppointmentDTO.java).
Mapper: backend/mapper/clinical/AppointmentMapper.java — toDTO(Appointment), toSummaryDTO(Appointment a, String patientName, String doctorName), toEntity(CreateAppointmentDTO).
TableController expects: Appointment entity (backend/model/patient/Appointment) — confirmed via AppointmentTableController.java import (note package is model.patient, not model.clinical).
Constructor pattern: new AppointmentServiceImpl(new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl()) (3-DAO ctor, used identically in PatientDetailController, LabOrdersController, InvoicePageController, MedicalRecordsController, ReferralsController, PrescriptionsController, EntityLookupService) — AppointmentServiceImpl(AppointmentDAO, PatientDAO, DoctorDAO).
4. DepartmentService
File: backend/service/department/interfaces/DepartmentService.java
Methods: create(CreateDepartmentDTO) : DepartmentDTO; findById(String) : DepartmentDTO; findAll() : List<DepartmentDTO> (plain list, no pagination at all); update(String, CreateDepartmentDTO) : DepartmentDTO; delete(String).
findAll DTO: DepartmentDTO (backend/dto/doctor/DepartmentDTO.java) — plain List, not PageResult.
Mapper: backend/mapper/doctor/DepartmentMapper.java — toDTO(Department), toEntity(CreateDepartmentDTO).
TableController expects: Department entity (backend/model/doctor/Department) — confirmed via DepartmentTableController.java import.
Constructor pattern: new DepartmentServiceImpl(new DepartmentDAOImpl()) (single-DAO, used in DoctorsPageController, EntityLookupService) — DepartmentServiceImpl(DepartmentDAO departmentDAO).
5. DoctorScheduleService
File: backend/service/department/interfaces/DoctorScheduleService.java
Methods: create(CreateDoctorScheduleDTO) : DoctorScheduleDTO; findByDoctor(String) : List<DoctorScheduleDTO> (no plain findAll, no pagination); update(String scheduleId, CreateDoctorScheduleDTO) : DoctorScheduleDTO; delete(String).
DTO: DoctorScheduleDTO (backend/dto/doctor/DoctorScheduleDTO.java).
Mapper: backend/mapper/doctor/DoctorScheduleMapper.java — toDTO(DoctorSchedule), toEntity(CreateDoctorScheduleDTO).
TableController expects: DoctorSchedule entity (backend/model/doctor/DoctorSchedule) — confirmed via DoctorScheduleTableController.java import.
Constructor pattern (not yet wired anywhere in pages; read directly from impl): new DoctorScheduleServiceImpl(new DoctorScheduleDAOImpl()) — DoctorScheduleServiceImpl(DoctorScheduleDAO scheduleDAO). DAO file: backend/dao/department/DoctorScheduleDAOImpl.java.
6. ReferralService
File: backend/service/department/interfaces/ReferralService.java
Methods: create(CreateReferralDTO) : ReferralDTO; findById(String) : ReferralDTO; findByAppointment(String) : List<ReferralDTO> (no plain findAll); updateStatus(String, String) : ReferralDTO; delete(String).
DTO: ReferralDTO (backend/dto/doctor/ReferralDTO.java).
Mapper: backend/mapper/doctor/ReferralMapper.java — toDTO(Referral), toEntity(CreateReferralDTO) (sets status to "pending" unconditionally on create).
TableController expects: Referral entity (backend/model/doctor/Referral) — confirmed via ReferralTableController.java import; page ReferralsController.java already wires AppointmentServiceImpl/DoctorServiceImpl but not yet ReferralService.
Constructor pattern: new ReferralServiceImpl(new ReferralDAOImpl()) — ReferralServiceImpl(ReferralDAO referralDAO). DAO: backend/dao/department/ReferralDAOImpl.java.
7. MedicalRecordService
File: backend/service/clinical/interfaces/MedicalRecordService.java
Methods: create(CreateMedicalRecordDTO) : MedicalRecordDTO; findById(String) : MedicalRecordDTO; findByAppointment(String) : MedicalRecordDTO (singular, not a list — 1:1 with appointment); update(String, CreateMedicalRecordDTO) : MedicalRecordDTO; delete(String). No findAll/list method at all — every record must be found per-appointment.
DTO: MedicalRecordDTO (backend/dto/clinical/MedicalRecordDTO.java).
Mapper: backend/mapper/clinical/MedicalRecordMapper.java — toDTO(MedicalRecord), toEntity(CreateMedicalRecordDTO).
TableController expects: MedicalRecord entity (backend/model/patient/MedicalRecord) — confirmed via MedicalRecordTableController.java import.
Constructor pattern: new MedicalRecordServiceImpl(new MedicalRecordDAOImpl()) — MedicalRecordServiceImpl(MedicalRecordDAO recordDAO). DAO: backend/dao/clinical/MedicalRecordDAOImpl.java. Note: page MedicalRecordsController.java already wires AppointmentServiceImpl but not MedicalRecordService — to populate a table list you'll need to iterate appointments and call findByAppointment per appointment (no batch list API exists).
8. LabService (covers LabOrder + LabResult)
File: backend/service/lab/interfaces/LabService.java
Methods: orderTest(CreateLabOrderDTO) : LabOrderDTO; findOrderById(String) : LabOrderDTO; findOrdersByAppointment(String) : List<LabOrderDTO>; recordResult(CreateLabResultDTO) : LabResultDTO; findResultByOrder(String) : LabResultDTO; deleteOrder(String). No global findAll for orders or results.
DTOs: LabOrderDTO, LabResultDTO (backend/dto/lab/).
Mappers: backend/mapper/lab/LabOrderMapper.java — toDTO(LabOrder), toEntity(CreateLabOrderDTO) (forces status to LabOrderStatus.ORDERED.getDbValue()); backend/mapper/lab/LabResultMapper.java — toDTO(LabResult), toEntity(CreateLabResultDTO).
TableController expects: LabOrder entity (backend/model/lab/LabOrder) — confirmed via LabOrderTableController.java import. (No separate LabResultTableController found.)
Constructor pattern: new LabServiceImpl(new LabOrderDAOImpl(), new LabResultDAOImpl()) — LabServiceImpl(LabOrderDAO labOrderDAO, LabResultDAO labResultDAO). DAOs: backend/dao/lab/LabOrderDAOImpl.java, backend/dao/lab/LabResultDAOImpl.java. Page LabOrdersController.java already wires AppointmentServiceImpl/DoctorServiceImpl but not LabService yet — to list "all" orders you'd need per-appointment iteration (no batch list endpoint exists).
9. PharmacyService (covers Medication + MedicalInventory)
File: backend/service/pharmacy/interfaces/PharmacyService.java
Methods: addMedication(CreateMedicationDTO) : MedicationDTO; findMedicationById(String) : MedicationDTO; findAllMedications() : List<MedicationDTO> (plain list); addStock(CreateMedicalInventoryDTO) : MedicalInventoryDTO; findStockByMedication(String) : List<MedicalInventoryDTO>; findLowStock() : List<MedicalInventoryDTO>; updateStock(String inventoryId, CreateMedicalInventoryDTO) : MedicalInventoryDTO.
DTOs: MedicationDTO, MedicalInventoryDTO (backend/dto/pharmacy/).
Mappers: backend/mapper/pharmacy/MedicationMapper.java — toDTO(Medication), toEntity(CreateMedicationDTO); backend/mapper/pharmacy/MedicalInventoryMapper.java — toDTO(MedicalInventory), toEntity(CreateMedicalInventoryDTO).
TableController expects: MedicalInventory entity (backend/model/pharmacy/MedicalInventory) — confirmed via MedicalInventoryTableController.java import. (No dedicated MedicationTableController found — medications are likely a picker/combo list inside PharmacyController/prescription flows, not a standalone paged table.)
Constructor pattern: new PharmacyServiceImpl(new MedicationDAOImpl(), new MedicalInventoryDAOImpl()) (already wired in PharmacyController.java, EntityLookupService.java) — PharmacyServiceImpl(MedicationDAO medicationDAO, MedicalInventoryDAO inventoryDAO).
10. PrescriptionService
File: backend/service/pharmacy/interfaces/PrescriptionService.java
Methods: issue(CreatePrescriptionDTO) : PrescriptionDTO; findById(String) : PrescriptionDTO; findByAppointment(String) : PrescriptionDTO; findByPatient(String) : List<PrescriptionDTO>; delete(String). No global findAll.
DTO: PrescriptionDTO (backend/dto/pharmacy/PrescriptionDTO.java) — note PrescriptionMapper.toDTO sets the items field to null ("items loaded separately by service").
Mapper: backend/mapper/pharmacy/PrescriptionMapper.java — toDTO(Prescription), toEntity(CreatePrescriptionDTO). (There's also PrescriptionItemMapper for line items, not read in detail but exists at backend/mapper/pharmacy/PrescriptionItemMapper.java.)
TableController expects: Prescription entity (backend/model/pharmacy/Prescription) — confirmed via PrescriptionTableController.java import.
Constructor pattern: new PrescriptionServiceImpl(new PrescriptionDAOImpl(), new PrescriptionItemDAOImpl()) — PrescriptionServiceImpl(PrescriptionDAO prescriptionDAO, PrescriptionItemDAO itemDAO). DAOs: backend/dao/pharmacy/PrescriptionDAOImpl.java, backend/dao/pharmacy/PrescriptionItemDAOImpl.java. Page PrescriptionsController.java already wires AppointmentServiceImpl + EntityLookupService but not PrescriptionService — listing "all" prescriptions needs per-patient/appointment iteration (no batch endpoint).
11. InvoiceService
File: backend/service/finance/interfaces/InvoiceService.java
Methods: generate(CreateInvoiceDTO) : InvoiceDTO; findById(String) : InvoiceDTO; findAll(PageRequest) : PageResult<InvoiceSummaryDTO>; findByPatient(String) : List<InvoiceDTO>; markPaid(String) : InvoiceDTO; delete(String).
findAll DTO: InvoiceSummaryDTO (backend/dto/finance/InvoiceSummaryDTO.java).
Mapper: backend/mapper/finance/InvoiceMapper.java — toDTO(Invoice), toSummaryDTO(Invoice, patientName), toEntity(CreateInvoiceDTO).
TableController expects: Invoice entity (backend/model/finance/Invoice) — confirmed via InvoiceTableController.java import.
Constructor pattern: new InvoiceServiceImpl(new InvoiceDAOImpl(), new PatientDAOImpl()) (page InvoicePageController.java already wires PatientServiceImpl/AppointmentServiceImpl but not InvoiceService yet) — InvoiceServiceImpl(InvoiceDAO invoiceDAO, PatientDAO patientDAO). DAO: backend/dao/finance/InvoiceDAOImpl.java.
12. UserService
File: backend/service/auth/interfaces/UserService.java
Methods: create(CreateUserDTO) : UserDTO; findById(String) : UserDTO; findAll(PageRequest) : PageResult<UserDTO>; update(UpdateUserDTO) : UserDTO; deactivate(String); delete(String).
findAll DTO: UserDTO (backend/dto/auth/UserDTO.java).
Mapper: backend/mapper/auth/UserMapper.java — toDTO(User), toEntity(CreateUserDTO) (forces isActive = true).
TableController expects: need to verify against UserTableController.java — not explicitly grepped for model import in the earlier batch, but by the codebase's consistent pattern it's User entity (backend/model/user/User); confirm before implementation (only class not directly confirmed in the model-import grep list, since that grep run returned matches for 16 of the controllers but not UserTableController/RoleTableController/PermissionTableController/UserSessionTableController — recommend a quick explicit check of those four files before coding).
Constructor pattern: new UserServiceImpl(new UserDAOImpl()) (already used in UsersPageController.java, ProfilePageController.java, EntityLookupService.java) — UserServiceImpl(UserDAO userDAO).
13. RoleService
File: backend/service/auth/interfaces/RoleService.java
Methods: create(CreateRoleDTO) : RoleDTO; findById(String) : RoleDTO; findAll() : List<RoleDTO> (plain list, no pagination); delete(String); plus assignment methods: assignToUser, revokeFromUser, findRolesForUser(String) : List<RoleDTO>, assignPermission, revokePermission, findPermissionsForRole(String) : List<PermissionDTO>.
findAll DTO: RoleDTO (backend/dto/auth/RoleDTO.java).
Mapper: backend/mapper/auth/RoleMapper.java — toDTO(Role), toEntity(CreateRoleDTO).
TableController expects: presumably Role entity (backend/model/user/Role) per RoleTableController.java — confirm directly (not in the earlier confirmed-import list; recommend verifying before coding, same caveat as UserService above).
Constructor pattern: new RoleServiceImpl(new RoleDAOImpl(), new UserRoleDAOImpl(), new RolePermissionDAOImpl(), new PermissionDAOImpl()) (4-DAO ctor, already used identically in UsersPageController.java, RolesPageController.java, ProfilePageController.java) — RoleServiceImpl(RoleDAO roleDAO, UserRoleDAO userRoleDAO, RolePermissionDAO rolePermissionDAO, PermissionDAO permissionDAO).
14. AuditService
File: backend/service/log/interfaces/AuditService.java
Methods: record(userId, action, table, recordId) : AuditLogDTO; findAll(PageRequest) : PageResult<AuditLogDTO>; findByUser(String) : List<AuditLogDTO>; purgeOlderThanDays(int) : int.
findAll DTO: AuditLogDTO (backend/dto/log/AuditLogDTO.java).
Mapper: backend/mapper/log/AuditLogMapper.java — toDTO(AuditLog) and toEntity(AuditLogDTO) (this one is DTO→Entity directly, not Create-DTO-based, since there's no CreateAuditLogDTO — an exception to the general pattern).
TableController expects: AuditLog entity (backend/model/user/AuditLog) — confirmed via AuditLogTableController.java import.
Constructor pattern: new AuditServiceImpl(new AuditLogDAOImpl()) — AuditServiceImpl(AuditLogDAO auditLogDAO). DAO: backend/dao/log/AuditLogDAOImpl.java (also used inside AuthServiceImpl's ctor chain elsewhere, e.g. new AuthServiceImpl(new UserDAOImpl(), new UserSessionDAOImpl(), new UserRoleDAOImpl(), new RoleDAOImpl(), new AuditLogDAOImpl()), but AuditServiceImpl itself is only single-DAO).
15. SystemLogService
File: backend/service/log/interfaces/SystemLogService.java
Methods: log(level, source, message, userId) : SystemLogDTO; findAll(PageRequest) : PageResult<SystemLogDTO>; findByLevel(String) : List<SystemLogDTO>; purgeOlderThanDays(int) : int.
findAll DTO: SystemLogDTO (backend/dto/log/SystemLogDTO.java).
Mapper: backend/mapper/log/SystemLogMapper.java — toDTO(SystemLog) and toEntity(SystemLogDTO) (same DTO→Entity exception as AuditLogMapper, no CreateSystemLogDTO exists).
TableController expects: SystemLog entity (backend/model/user/SystemLog) — confirmed via SystemLogTableController.java import.
Constructor pattern: new SystemLogServiceImpl(new SystemLogDAOImpl()) — SystemLogServiceImpl(SystemLogDAO systemLogDAO). DAO: backend/dao/log/SystemLogDAOImpl.java.
16. AllergyService
File: backend/service/patient/interfaces/AllergyService.java
Methods: add(CreatePatientAllergyDTO) : PatientAllergyDTO; findByPatient(String) : List<PatientAllergyDTO>; delete(String). No global findAll.
DTO: PatientAllergyDTO (backend/dto/patient/PatientAllergyDTO.java).
Mapper: backend/mapper/patient/PatientAllergyMapper.java — toDTO(PatientAllergy), toEntity(CreatePatientAllergyDTO).
TableController expects: PatientAllergy entity (backend/model/patient/PatientAllergy) — confirmed via PatientAllergyTableController.java import.
Constructor pattern: new AllergyServiceImpl(new PatientAllergyDAOImpl()) — AllergyServiceImpl(PatientAllergyDAO allergyDAO). DAO: backend/dao/patient/PatientAllergyDAOImpl.java. Listing for a page would need per-patient calls (e.g. iterate all patients via PatientService, or is likely used only on a patient-detail drill-down tab, matching PatientAllergyTableController's likely use inside PatientDetailController).
17. FeedbackService
File: backend/service/patient/interfaces/FeedbackService.java
Methods: submit(CreatePatientFeedbackDTO) : PatientFeedbackDTO; findByPatient(String) : List<PatientFeedbackDTO>; delete(String). No global findAll.
DTO: PatientFeedbackDTO (backend/dto/patient/PatientFeedbackDTO.java).
Mapper: backend/mapper/patient/PatientFeedbackMapper.java — toDTO(PatientFeedback), toEntity(CreatePatientFeedbackDTO).
TableController expects: No dedicated FeedbackTableController was found in the pages/components/**/*TableController*.java glob results — feedback likely isn't shown via a standalone paged table (verify with a targeted search for "Feedback" under pages/ before assuming a table page exists for it).
Constructor pattern: new FeedbackServiceImpl(new PatientFeedbackDAOImpl()) — FeedbackServiceImpl(PatientFeedbackDAO feedbackDAO). DAO: backend/dao/patient/PatientFeedbackDAOImpl.java.
18. VitalSignService
File: backend/service/patient/interfaces/VitalSignService.java
Methods: record(CreateVitalSignDTO) : VitalSignDTO; findByAppointment(String) : VitalSignDTO; findByPatient(String) : List<VitalSignDTO>; delete(String). No global findAll.
DTO: VitalSignDTO (backend/dto/patient/VitalSignDTO.java).
Mapper: backend/mapper/patient/VitalSignMapper.java — toDTO(VitalSign), toEntity(CreateVitalSignDTO).
TableController expects: VitalSign entity (backend/model/patient/VitalSign) — confirmed via VitalSignTableController.java import.
Constructor pattern: new VitalSignServiceImpl(new VitalSignDAOImpl()) — VitalSignServiceImpl(VitalSignDAO vitalSignDAO). DAO: backend/dao/patient/VitalSignDAOImpl.java. Listing needs per-patient findByPatient calls (this table is almost certainly a patient-detail drill-down tab, not a standalone all-patients-vitals page, matching how AllergyService/FeedbackService/VitalSignService are all patient-scoped read APIs).
Pagination/PageResult answer (consolidated)
PageResult<T> (backend/utils/pagination/PageResult.java) exposes getItems(), getNextCursor(), hasMore(), getPageSize(), getCount(), isEmpty(), and a generic map(Function<T,R>) for DTO conversion at the service layer — but no "give me everything" method.
CursorPagination.firstPage(int size) (line 48 of CursorPagination.java) signature: public static PageRequest firstPage(int size), building new PageRequest(null, size, PageRequest.SortDirection.DESC). There's also parameterless firstPage() using AppConfig.getPageSize() as the default, and nextPage(cursor)/nextPage(cursor, size) for subsequent pages.
No *ServiceImpl.findAll() ignores pagination for the ones that take a PageRequest — they all delegate to DAO-level cursor pagination (CursorPagination.toResult(...)) with real LIMIT/cursor semantics. Services without a PageRequest-based findAll (DepartmentService.findAll(), RoleService.findAll(), PharmacyService.findAllMedications()) genuinely return an unpaginated List<DTO> directly — those are the only truly "give me all" APIs in the whole service layer.
Practical implication for the implementation plan: for services with PageRequest-based findAll (Patient, Doctor, Appointment→summary, Invoice→summary, User, AuditLog, SystemLog), the page controller must either (a) call findAll(CursorPagination.firstPage(SOME_LARGE_SIZE)) once and treat that as "all" (simplest, but technically caps at that size), or (b) loop calling nextPage(result.getNextCursor()) until hasMore() is false, concatenating getItems() each time, then hand the full concatenated List to PaginatedTableController.setItems(...) — since that controller re-paginates client-side anyway at 10 rows/page regardless of what's handed to it.
Confirmed: PaginatedTableController (pages/components/PaginatedTableController.java) does not touch the service layer at all — it is purely a display/filter/paging widget driven by whatever List<T> the page controller (e.g. PatientsPageController.refreshTable() calling patientTableController.setItems(patients)) hands it. All service-calling logic must live in the page controllers (pages/<domain>/*PageController.java / *Controller.java), consistent with the existing partial-wiring pattern (EntityLookupService, AppointmentServiceImpl usages, etc.).