Objective
- Integrate all JavaFX page controllers in the hospital-management-system with the already-tested backend service layer (PostgreSQL). All backend code exists — no new code; only wire existing pages to existing services (auth Users/Roles pages are the only ones already integrated).
Important Details
- Repo: C:\Users\Ishimwe-Conscience\Documents\amalitech-systems\amalitech-labs\hospital-management-system; Java 25.0.2; Maven wrapper .\mvnw.cmd (use .\mvnw.cmd clean compile — plain compile lies due to stale incremental state).
- Ordering guide: development-priority.md (Phase 0 foundation → Phase 7 logs). CurrentReport.md in root holds per-service method/constructor breakdown.
- Integration pattern (from UsersPageController/DoctorsPageController/DepartmentsPageController + UserTableController):
- Table controllers: retype generic + columns + matches(...) from Entity → DTO.
- Page controllers: private final XService service = new XServiceImpl(new XDAOImpl());; list via findAll(CursorPagination.firstPage(500)).getItems(); no global findAll → iterate parent rows (appointments) calling per-parent lookup; form dialog saves via Create/Update DTO in try/catch (AppException → formDialogController.setError(...) + setLoading(false)); delete wraps service.delete(id).
- Do NOT modify: PaginatedTableController, BasePageController, .fxml, backend, EntityLookupService. Preserve UI/behaviors; only swap data source.
- Earlier context (stale, resolved): OpenCode payment question answered (OpenCode Zen via /connect → opencode.ai/auth); the auth error was a server-side failure (insert into account query failed) — just retry.
- DTO notes: PrescriptionDTO.items is null from mapper — don't rely on it; AppointmentTableController/InvoiceTableController must target Summary DTOs (no patientId/doctorId/reason getters); LabOrderDTO has no results field.
- Service constructors verified: AppointmentServiceImpl( AppointmentDAOImpl, PatientDAOImpl, DoctorDAOImpl); PrescriptionServiceImpl(PrescriptionDAOImpl, PrescriptionItemDAOImpl); LabServiceImpl(LabOrderDAOImpl, LabResultDAOImpl); MedicalRecordServiceImpl(MedicalRecordDAOImpl); DoctorServiceImpl(DoctorDAOImpl, DepartmentDAOImpl); DoctorScheduleServiceImpl(DoctorScheduleDAOImpl); AuditServiceImpl(AuditLogDAOImpl); SystemLogServiceImpl(SystemLogDAOImpl).
Work State
Completed
- Departments: DepartmentTableController → DepartmentDTO; DepartmentsPageController → DepartmentServiceImpl(new DepartmentDAOImpl()).
- Doctors: DoctorTableController → DoctorDTO; DoctorsPageController → DoctorServiceImpl + async department dropdown.
- Patients: PatientsPageController → PatientService; PatientTableController, PatientAllergyTableController, VitalSignTableController → DTO-typed.
- PatientDetail: wired Patient/VitalSign/Allergy services; loadPatient(Patient) changed to loadPatient(PatientDTO).
- Schedules: ScheduleController → DoctorScheduleService; day names mapped to/from DB 3-letter codes (CHECK constraint); DoctorScheduleTableController → DTO.
- MedicalRecordsController → MedicalRecordService, iterates appointments via findByAppointment; MedicalRecordTableController → MedicalRecordDTO.
- ReferralsController → ReferralService (+updateStatus); ReferralTableController → DTO.
- Logs: AuditLogsController/SystemLogsController/RetentionSettingsController wired to AuditService/SystemLogService (purgeOlderThanDays); AuditLogTableController/SystemLogTableController → DTO (id binds logId).
- .\mvnw.cmd clean compile currently FAILS (see Blocked) — first clean compile after agents; incremental compile previously passed.
Active
- Per-development-priority, remaining unintegrated: Appointments (Phase 2), Prescriptions (Phase 4), Lab Orders (Phase 5), Invoices (Phase 6).
- Prescription/Lab subagent returned empty (no changes) — must be done directly.
- Dashboard/Analytics pages are chart placeholders with no backend service to wire — left out of scope.
- MedicalRecordsController/Appointments/Invoices page controllers were read but not yet edited by me.
Blocked
- Compile errors — .\mvnw.cmd clean compile:
- PatientDetailController.java:[240,53] and [389,57] — List<MedicalRecord> cannot be converted to List<MedicalRecordDTO> (agent retyped MedicalRecordTableController to DTO but PatientDetail's medical-records tab still passes entities).
Next Move
1. Fix PatientDetailController lines 240/389: wire medical-records tab to MedicalRecordService.findByAppointment(...) (or otherwise produce List<MedicalRecordDTO>) — read the file's surrounding tab setup first; also check whether the same file touches Appointment/Prescription/Lab/Invoice tab table controllers before retyping those (will break call sites).
2. Integrate Prescriptions: PrescriptionTableController → PrescriptionDTO; PrescriptionsController → PrescriptionService iterating appointmentService.findAll(...) + findByAppointment.
3. Integrate Lab: LabOrderTableController → LabOrderDTO (keep status-change callback); LabOrdersController → LabService.
4. Integrate Appointments: AppointmentTableController → AppointmentSummaryDTO (drop reason/ID columns or adapt cell factories; caller PatientDetailController must be fixed in step 1); AppointmentsPageController → AppointmentService (book via CreateAppointmentDTO, update via UpdateAppointmentDTO, status change via updateStatus).
5. Integrate Invoices: InvoiceTableController → InvoiceSummaryDTO; InvoicePageController → InvoiceService (generate(CreateInvoiceDTO), markPaid for status change, findAll(CursorPagination.firstPage(500))).
6. Final verify: .\mvnw.cmd clean compile from the repo root.
Relevant Files
- development-priority.md: build-order guidance (Phases 0–7) used to sequence integration.
- CurrentReport.md (root, untracked): per-service method/constructor summary; pagination gap note (no "get all" helper → use CursorPagination.firstPage(500)).
- src/main/java/hospital/management/pages/doctor/DepartmentsPageController.java, auth/UsersPageController.java, doctor/DoctorsPageController.java, components/auth/UserTableController.java: integration reference templates.
- src/main/java/hospital/management/pages/patient/PatientDetailController.java: currently failing compile (lines 240/389) — medical-records/in-memory tabs not fully wired; edit carefully.
▣  Compaction · Big Pickle · 39.1s
+ Thought: 2.3s
# Todos
[ ] Fix PatientDetailController medical-records tab (lines 240/389) + other in-memory tabs
[ ] Integrate Appointments (AppointmentTableController -> DTO, AppointmentsPageController -> AppointmentService)
[ ] Integrate Invoices (InvoiceTableController -> DTO, InvoicePageController -> InvoiceService)
[ ] Integrate Prescriptions (PrescriptionTableController -> DTO, PrescriptionsController -> PrescriptionService)
[ ] Integrate Lab (LabOrderTableController -> DTO, LabOrdersController -> LabService)
[ ] Complete PharmacyController inventory CRUD + pending prescriptions (MedicalInventoryTableController -> DTO)
[ ] Verify: .\mvnw.cmd clean compile
+ Thought: 183ms
→Read src\main\java\hospital\management\pages\components\pharmacy\PrescriptionTableController.java 
→Read src\main\java\hospital\management\pages\pharmacy\PrescriptionsController.java 
→Read src\main\java\hospital\management\backend\service\pharmacy\interfaces\PrescriptionService.java 
→Read src\main\java\hospital\management\backend\dto\pharmacy\PrescriptionDTO.java 
+ Thought: 549ms
→Read src\main\java\hospital\management\pages\components\lab\LabOrderTableController.java 
→Read src\main\java\hospital\management\pages\lab\LabOrdersController.java 
→Read src\main\java\hospital\management\backend\service\lab\interfaces\LabService.java 
→Read src\main\java\hospital\management\backend\dto\lab\LabOrderDTO.java 
+ Thought: 1.3s
→Read src\main\java\hospital\management\backend\service\lab\LabServiceImpl.java 
→Read src\main\java\hospital\management\backend\dao\lab\LabOrderDAOImpl.java 
→Read src\main\java\hospital\management\backend\dto\lab\CreateLabResultDTO.java 
+ Thought: 8.9s
→Read src\main\java\hospital\management\pages\components\PaginatedTableController.java 
→Read src\main\java\hospital\management\backend\model\enums\LabOrderStatus.java 
→Read src\main\java\hospital\management\backend\dto\lab\CreateLabOrderDTO.java 
+ Thought: 20.5s
Now let me look at already-integrated controllers to match the exact async/refresh patterns used by the subagents.
→Read src\main\java\hospital\management\pages\clinical\MedicalRecordsController.java 
→Read src\main\java\hospital\management\pages\auth\UsersPageController.java 
+ Thought: 1.2s
The pattern is clear. Now let me read the appointment, invoice, and patient detail files.
→Read src\main\java\hospital\management\pages\components\clinical\AppointmentTableController.java 
→Read src\main\java\hospital\management\pages\clinical\AppointmentsPageController.java 
→Read src\main\java\hospital\management\pages\components\finance\InvoiceTableController.java 
→Read src\main\java\hospital\management\pages\finance\InvoicePageController.java 
+ Thought: 300ms
Now the clinical/finance service interfaces, DTOs, and PatientDetailController.
→Read src\main\java\hospital\management\backend\service\clinical\interfaces\AppointmentService.java 
→Read src\main\java\hospital\management\backend\service\finance\interfaces\InvoiceService.java 
→Read src\main\java\hospital\management\backend\dto\clinical\AppointmentSummaryDTO.java 
→Read src\main\java\hospital\management\backend\dto\finance\InvoiceSummaryDTO.java 
+ Thought: 1.1s
→Read src\main\java\hospital\management\backend\service\finance\InvoiceServiceImpl.java [limit=80]
→Read src\main\java\hospital\management\backend\service\clinical\AppointmentServiceImpl.java [limit=90]
→Read src\main\java\hospital\management\backend\dto\clinical\CreateAppointmentDTO.java 
→Read src\main\java\hospital\management\backend\dto\clinical\UpdateAppointmentDTO.java 
→Read src\main\java\hospital\management\backend\dto\finance\CreateInvoiceDTO.java 
+ Thought: 1.1s
→Read src\main\java\hospital\management\backend\model\enums\AppointmentStatus.java 
→Read src\main\java\hospital\management\backend\dto\clinical\AppointmentDTO.java 
→Read src\main\java\hospital\management\backend\dto\finance\InvoiceDTO.java 
→Read src\main\java\hospital\management\backend\dto\pharmacy\CreatePrescriptionDTO.java 
→Read src\main\java\hospital\management\backend\service\pharmacy\PrescriptionServiceImpl.java [limit=60]
+ Thought: 958ms
Now the critical PatientDetailController.
→Read src\main\java\hospital\management\pages\patient\PatientDetailController.java 
+ Thought: 27.4s
Let me check the FXML files for the table columns and page structures.
→Read src\main\resources\hospital\management\frontend\components\table\appointment-table.fxml 
→Read src\main\resources\hospital\management\frontend\components\table\invoice-table.fxml 
→Read src\main\resources\hospital\management\frontend\components\table\lab-order-table.fxml 
→Read src\main\resources\hospital\management\frontend\components\table\prescription-table.fxml 
+ Thought: 25.3s
→Read src\main\java\hospital\management\pages\BasePageController.java 
→Read src\main\java\hospital\management\pages\components\shared\feedback\FormDialogController.java 
→Read src\main\java\hospital\management\backend\model\enums\InvoiceStatus.java 
+ Thought: 617ms
✱Grep "payment_status|paymentStatus|PaymentStatus" in src\main\java\hospital\management\backend\model (14 matches)
✱Grep "enum" in src\main\java\hospital\management\backend\model (20 matches)
→Read src\main\java\hospital\management\backend\model\enums\PaymentStatus.java 
→Read src\main\java\hospital\management\backend\dto\pharmacy\CreatePrescriptionItemDTO.java 
+ Thought: 40.7s
Let me read the remaining backend pieces I need: AppointmentServiceImpl (cancel), InvoiceServiceImpl (markPaid), PrescriptionDAOImpl, MedicalInventoryDAOImpl, MedicationDTO.
→Read src\main\java\hospital\management\backend\service\clinical\AppointmentServiceImpl.java [offset=90, limit=80]
→Read src\main\java\hospital\management\backend\service\finance\InvoiceServiceImpl.java [offset=78, limit=62]
→Read src\main\java\hospital\management\backend\dao\pharmacy\PrescriptionDAOImpl.java 
+ Thought: 1.8s
→Read src\main\java\hospital\management\backend\service\pharmacy\PrescriptionServiceImpl.java [offset=60, limit=90]
→Read src\main\java\hospital\management\backend\dao\pharmacy\MedicalInventoryDAOImpl.java 
+ Thought: 3.8s
I have everything I need. Let me start implementing. First, the table controllers (retype generic from entity to DTO).
→Read src\main\java\hospital\management\backend\dto\pharmacy\MedicationDTO.java [limit=40]
+ Thought: 542ms
Now I'll implement. Starting with the table controllers.
← Edit src\main\java\hospital\management\pages\components\clinical\AppointmentTableController.java
package hospital.management.pages.components.clinical;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class AppointmentTableController extends PaginatedTableController<Appointment> {
public class AppointmentTableController extends PaginatedTableController<AppointmentDTO> {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<Appointment, String> patientIdColumn;
    @FXML private TableColumn<Appointment, String> doctorIdColumn;
    @FXML private TableColumn<Appointment, String> dateColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;
    @FXML private TableColumn<Appointment, Void>   changeStatusColumn;
    @FXML private TableColumn<Appointment, String> reasonColumn;
    @FXML private TableColumn<Appointment, Void>   actionsColumn;
    @FXML private TableColumn<AppointmentDTO, String> patientIdColumn;
    @FXML private TableColumn<AppointmentDTO, String> doctorIdColumn;
    @FXML private TableColumn<AppointmentDTO, String> dateColumn;
    @FXML private TableColumn<AppointmentDTO, String> statusColumn;
    @FXML private TableColumn<AppointmentDTO, Void>   changeStatusColumn;
    @FXML private TableColumn<AppointmentDTO, String> reasonColumn;
    @FXML private TableColumn<AppointmentDTO, Void>   actionsColumn;

    private Consumer<Appointment> onChangeStatus;
    private Consumer<AppointmentDTO> onChangeStatus;

    /** Registers the row-level "change status" callback used by the changeStatusColumn button. */
    public void setOnChangeStatus(Consumer<Appointment> onChangeStatus) {
    public void setOnChangeStatus(Consumer<AppointmentDTO> onChangeStatus) {
        this.onChangeStatus = onChangeStatus;
    }

    @Override
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(Appointment appointment, String lowerQuery) {
    protected boolean matches(AppointmentDTO appointment, String lowerQuery) {
        return containsIgnoreCase(appointment.getPatientId(), lowerQuery)
                || containsIgnoreCase(appointment.getDoctorId(), lowerQuery)
                || containsIgnoreCase(appointment.getStatus(), lowerQuery)
                || containsIgnoreCase(appointment.getReason(), lowerQuery);
← Edit src\main\java\hospital\management\pages\components\finance\InvoiceTableController.java
package hospital.management.pages.components.finance;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.dto.finance.InvoiceDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class InvoiceTableController extends PaginatedTableController<Invoice> {
public class InvoiceTableController extends PaginatedTableController<InvoiceDTO> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<Invoice, String>     invoiceIdColumn;
    @FXML private TableColumn<Invoice, String>     patientIdColumn;
    @FXML private TableColumn<Invoice, String>     appointmentIdColumn;
    @FXML private TableColumn<Invoice, BigDecimal> totalAmountColumn;
    @FXML private TableColumn<Invoice, String>     paymentStatusColumn;
    @FXML private TableColumn<Invoice, Void>       changeStatusColumn;
    @FXML private TableColumn<Invoice, String>     issuedAtColumn;
    @FXML private TableColumn<Invoice, Void>       actionsColumn;
    @FXML private TableColumn<InvoiceDTO, String>     invoiceIdColumn;
    @FXML private TableColumn<InvoiceDTO, String>     patientIdColumn;
    @FXML private TableColumn<InvoiceDTO, String>     appointmentIdColumn;
    @FXML private TableColumn<InvoiceDTO, BigDecimal> totalAmountColumn;
    @FXML private TableColumn<InvoiceDTO, String>     paymentStatusColumn;
    @FXML private TableColumn<InvoiceDTO, Void>       changeStatusColumn;
    @FXML private TableColumn<InvoiceDTO, String>     issuedAtColumn;
    @FXML private TableColumn<InvoiceDTO, Void>       actionsColumn;

    private Consumer<Invoice> onChangeStatus;
    private Consumer<InvoiceDTO> onChangeStatus;

    public void setOnChangeStatus(Consumer<Invoice> onChangeStatus) {
    public void setOnChangeStatus(Consumer<InvoiceDTO> onChangeStatus) {
        this.onChangeStatus = onChangeStatus;
    }

    @Override
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(Invoice invoice, String lowerQuery) {
    protected boolean matches(InvoiceDTO invoice, String lowerQuery) {
        String patientId = invoice.getPatientId();
        String paymentStatus = invoice.getPaymentStatus();
        return (patientId != null && patientId.toLowerCase().contains(lowerQuery))
                || (paymentStatus != null && paymentStatus.toLowerCase().contains(lowerQuery));
← Edit src\main\java\hospital\management\pages\components\pharmacy\PrescriptionTableController.java
package hospital.management.pages.components.pharmacy;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.pharmacy.Prescription;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class PrescriptionTableController extends PaginatedTableController<Prescription> {
public class PrescriptionTableController extends PaginatedTableController<PrescriptionDTO> {

    @FXML private TableColumn<Prescription, String> prescriptionIdColumn;
    @FXML private TableColumn<Prescription, String> appointmentIdColumn;
    @FXML private TableColumn<Prescription, java.time.LocalDate> dateIssuedColumn;
    @FXML private TableColumn<Prescription, Void>   actionsColumn;
    @FXML private TableColumn<PrescriptionDTO, String> prescriptionIdColumn;
    @FXML private TableColumn<PrescriptionDTO, String> appointmentIdColumn;
    @FXML private TableColumn<PrescriptionDTO, java.time.LocalDate> dateIssuedColumn;
    @FXML private TableColumn<PrescriptionDTO, Void>   actionsColumn;

    @Override
    protected void configureColumns() {
        prescriptionIdColumn.setCellValueFactory(new PropertyValueFactory<>("prescriptionId"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(Prescription prescription, String lowerQuery) {
    protected boolean matches(PrescriptionDTO prescription, String lowerQuery) {
        return (prescription.getPrescriptionId() != null && prescription.getPrescriptionId().toLowerCase().contains(lowerQuery))
                || (prescription.getAppointmentId() != null && prescription.getAppointmentId().toLowerCase().contains(lowerQuery));
    }
}
Now the LabOrderTableController → LabOrderDTO (with status label mapping):
← Edit src\main\java\hospital\management\pages\components\lab\LabOrderTableController.java
package hospital.management.pages.components.lab;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.lab.LabOrder;
import hospital.management.backend.dto.lab.LabOrderDTO;
import hospital.management.backend.model.enums.LabOrderStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class LabOrderTableController extends PaginatedTableController<LabOrder> {
public class LabOrderTableController extends PaginatedTableController<LabOrderDTO> {

    private static final DateTimeFormatter ORDERED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<LabOrder, String> idColumn;
    @FXML private TableColumn<LabOrder, String> doctorIdColumn;
    @FXML private TableColumn<LabOrder, String> testNameColumn;
    @FXML private TableColumn<LabOrder, String> statusColumn;
    @FXML private TableColumn<LabOrder, Void>   changeStatusColumn;
    @FXML private TableColumn<LabOrder, String> orderedAtColumn;
    @FXML private TableColumn<LabOrder, Void>   actionsColumn;
    @FXML private TableColumn<LabOrderDTO, String> idColumn;
    @FXML private TableColumn<LabOrderDTO, String> doctorIdColumn;
    @FXML private TableColumn<LabOrderDTO, String> testNameColumn;
    @FXML private TableColumn<LabOrderDTO, String> statusColumn;
    @FXML private TableColumn<LabOrderDTO, Void>   changeStatusColumn;
    @FXML private TableColumn<LabOrderDTO, String> orderedAtColumn;
    @FXML private TableColumn<LabOrderDTO, Void>   actionsColumn;

    private Consumer<LabOrder> onChangeStatus;
    private Consumer<LabOrderDTO> onChangeStatus;

    public void setOnChangeStatus(Consumer<LabOrder> onChangeStatus) {
    public void setOnChangeStatus(Consumer<LabOrderDTO> onChangeStatus) {
        this.onChangeStatus = onChangeStatus;
    }

    @Override
    protected void configureColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("labOrderId"));
        doctorIdColumn.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        testNameColumn.setCellValueFactory(new PropertyValueFactory<>("testName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(statusLabel(cell.getValue().getStatus())));
        wireSingleActionColumn(changeStatusColumn, "fas-flag",
                item -> { if (onChangeStatus != null) onChangeStatus.accept(item); });
        orderedAtColumn.setCellValueFactory(cell -> {
            var orderedAt = cell.getValue().getOrderedAt();
        });
        wireActionsColumn(actionsColumn);
    }

    private static String statusLabel(String status) {
        try {
            return LabOrderStatus.fromDbValue(status).getLabel();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }

    @Override
    protected boolean matches(LabOrder order, String lowerQuery) {
    protected boolean matches(LabOrderDTO order, String lowerQuery) {
        String testName = order.getTestName();
        String status = order.getStatus();
        String status = statusLabel(order.getStatus());
        return (testName != null && testName.toLowerCase().contains(lowerQuery))
                || (status != null && status.toLowerCase().contains(lowerQuery));
    }
}
Now the MedicalInventoryTableController → MedicalInventoryDTO:
← Edit src\main\java\hospital\management\pages\components\pharmacy\MedicalInventoryTableController.java
package hospital.management.pages.components.pharmacy;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.pharmacy.MedicalInventory;
import hospital.management.backend.dto.pharmacy.MedicalInventoryDTO;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

 * Reused for BOTH the Inventory tab (all rows) and the Low Stock tab
 * (a pre-filtered subset). Filtering by reorder level happens at the page
 * controller level — this class only renders whatever list it's given.
 */
public class MedicalInventoryTableController extends PaginatedTableController<MedicalInventory> {
public class MedicalInventoryTableController extends PaginatedTableController<MedicalInventoryDTO> {

    @FXML private TableColumn<MedicalInventory, String>    inventoryIdColumn;
    @FXML private TableColumn<MedicalInventory, String>    medicationIdColumn;
    @FXML private TableColumn<MedicalInventory, String>    batchNumberColumn;
    @FXML private TableColumn<MedicalInventory, java.time.LocalDate> expiryDateColumn;
    @FXML private TableColumn<MedicalInventory, Integer>   quantityInStockColumn;
    @FXML private TableColumn<MedicalInventory, Integer>   reorderLevelColumn;
    @FXML private TableColumn<MedicalInventory, String>    supplierColumn;
    @FXML private TableColumn<MedicalInventory, Void>      actionsColumn;
    @FXML private TableColumn<MedicalInventoryDTO, String>    inventoryIdColumn;
    @FXML private TableColumn<MedicalInventoryDTO, String>    medicationIdColumn;
    @FXML private TableColumn<MedicalInventoryDTO, String>    batchNumberColumn;
    @FXML private TableColumn<MedicalInventoryDTO, java.time.LocalDate> expiryDateColumn;
    @FXML private TableColumn<MedicalInventoryDTO, Integer>   quantityInStockColumn;
    @FXML private TableColumn<MedicalInventoryDTO, Integer>   reorderLevelColumn;
    @FXML private TableColumn<MedicalInventoryDTO, String>    supplierColumn;
    @FXML private TableColumn<MedicalInventoryDTO, Void>      actionsColumn;

    @Override
    protected void configureColumns() {
        inventoryIdColumn.setCellValueFactory(new PropertyValueFactory<>("inventoryId"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(MedicalInventory item, String lowerQuery) {
    protected boolean matches(MedicalInventoryDTO item, String lowerQuery) {
        return (item.getMedicationId() != null && item.getMedicationId().toLowerCase().contains(lowerQuery))
                || (item.getBatchNumber() != null && item.getBatchNumber().toLowerCase().contains(lowerQuery))
                || (item.getSupplier() != null && item.getSupplier().toLowerCase().contains(lowerQuery));
    }
+ Thought: 6.1s
Now the AppointmentsPageController:
← Edit src\main\java\hospital\management\pages\clinical\AppointmentsPageController.java
package hospital.management.pages.clinical;

import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.clinical.CreateAppointmentDTO;
import hospital.management.backend.dto.clinical.UpdateAppointmentDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.model.enums.AppointmentStatus;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.clinical.interfaces.AppointmentService;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.utils.pagination.CursorPagination;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AppointmentsPageController extends BasePageController implements QuickAddCapable {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentService appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final PatientServiceImpl patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final DoctorServiceImpl doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private AppointmentTableController appointmentTableController;

    @FXML private Button addAppointmentBtn;

    private final List<Appointment> appointments = new ArrayList<>();
    private final List<AppointmentDTO> appointments = new ArrayList<>();
    private LocalDate selectedDate;

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.APPOINTMENTS);

        refreshTable();
    }

    private void loadAppointmentsForDate(LocalDate date) {
        // Backend service layer is stubbed; date-based filtering will be wired
        // once appointment lookups are backed by a real service.
        refreshTable();
        selectedDate = date;
        applyFilter();
    }

    private void refreshTable() {
        appointmentTableController.setItems(appointments);
        try {
            appointments.clear();
            List<AppointmentSummaryDTO> summaries =
                    appointmentService.findAll(CursorPagination.firstPage(500)).getItems();
            for (AppointmentSummaryDTO summary : summaries) {
                appointments.add(appointmentService.findById(summary.getAppointmentId()));
            }
            selectedDate = null;
            applyFilter();
        } catch (Exception e) {
            toastError("Failed to load appointments: " + e.getMessage());
        }
    }

    private void viewAppointmentDetail(Appointment appointment) {
    private void applyFilter() {
        List<AppointmentDTO> visible = appointments.stream()
                .filter(a -> selectedDate == null
                        || (a.getAppointmentDate() != null
                            && a.getAppointmentDate().toLocalDate().equals(selectedDate)))
                .toList();
        appointmentTableController.setItems(visible);
    }

    private void viewAppointmentDetail(AppointmentDTO appointment) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Patient", entityLookupService.patientLabel(appointment.getPatientId()));
            fields.put("Doctor", entityLookupService.doctorLabel(appointment.getDoctorId()));
        } catch (Exception ex) {
            toastError("Failed to resolve appointment details: " + ex.getMessage());
        }
        fields.put("Date/Time", appointment.getAppointmentDate() == null ? null : appointment.getAppointmentDate().toString());
        fields.put("Status", appointment.getStatus());
        fields.put("Status", statusLabel(appointment.getStatus()));
        fields.put("Reason", appointment.getReason());
        detailViewController.show("Appointment Details", "fas-calendar-check", fields);
    }

    private void confirmDeleteAppointment(Appointment appointment) {
        confirm("Delete Appointment",
                "Are you sure you want to delete this appointment? This cannot be undone.",
    private void confirmDeleteAppointment(AppointmentDTO appointment) {
        confirm("Cancel Appointment",
                "Are you sure you want to cancel this appointment? This cannot be undone.",
                () -> {
                    appointments.remove(appointment);
                    refreshTable();
                    toastSuccess("Appointment deleted.");
                    try {
                        appointmentService.cancel(appointment.getAppointmentId());
                        refreshTable();
                        toastSuccess("Appointment cancelled.");
                    } catch (Exception e) {
                        toastError("Failed to cancel appointment: " + e.getMessage());
                    }
                });
    }

    @Override
        openAppointmentDialog(null);
    }

    /** Opens the shared form dialog in Add mode (appointment == null) or Update mode. */
    private void openAppointmentDialog(Appointment appointment) {
    private void openAppointmentDialog(AppointmentDTO appointment) {
        boolean addMode = appointment == null;

        LoadingIdComboBox patientIdField = new LoadingIdComboBox();
        LoadingIdComboBox doctorIdField  = new LoadingIdComboBox();
                formDialogController.setLoading(false);
                return;
            }

            Appointment target = addMode ? new Appointment() : appointment;
            if (addMode) {
                target.setAppointmentId(UUID.randomUUID().toString());
                target.setStatus("Scheduled");
            try {
                LocalDateTime dateTime = LocalDateTime.of(appointmentDate.getValue(), time);
                if (addMode) {
                    appointmentService.book(new CreateAppointmentDTO(pId, dId, dateTime, reason.getText()));
                } else {
                    appointmentService.update(new UpdateAppointmentDTO(
                            appointment.getAppointmentId(), dateTime, appointment.getStatus(), reason.getText()));
                }
                refreshTable();
                formDialogController.close();
                toastSuccess(addMode ? "Appointment added." : "Appointment updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save appointment: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
            target.setPatientId(pId);
            target.setDoctorId(dId);
            target.setAppointmentDate(LocalDateTime.of(appointmentDate.getValue(), time));
            target.setReason(reason.getText());

            if (addMode) appointments.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Appointment added." : "Appointment updated.");
        });

        formDialogController.addField("Patient", "fas-user-injured", patientIdField);
        formDialogController.addField("Doctor", "fas-user-md", doctorIdField);
    /** Loads the patient/doctor dropdown options asynchronously, showing each dropdown's own
     *  spinner while its data is in flight and keeping the rest of the form disabled until
     *  both have finished loading. */
    private void loadAppointmentDropdowns(LoadingIdComboBox patientIdField, LoadingIdComboBox doctorIdField,
                                           List<Control> otherFields, Appointment existing) {
                                           List<Control> otherFields, AppointmentDTO existing) {
        EntityIdComboBox patientId = patientIdField.getComboBox();
        EntityIdComboBox doctorId = doctorIdField.getComboBox();

        patientIdField.setLoading(true);
            });
    }

    /** Minimal single-field dialog for changing an existing appointment's status, kept out of the main Add/Edit form. */
    private void openAppointmentStatusDialog(Appointment appointment) {
    private void openAppointmentStatusDialog(AppointmentDTO appointment) {
        ComboBox<String> status = new ComboBox<>();
        status.getStyleClass().add("form-combo");
        status.getItems().addAll("Scheduled", "Completed", "Cancelled", "No-show");
        status.setValue(appointment.getStatus());
        for (AppointmentStatus s : AppointmentStatus.values()) {
            status.getItems().add(s.getLabel());
        }
        status.setValue(statusLabel(appointment.getStatus()));

        formDialogController.open("Change Status", "fas-info-circle", false, v -> {
            if (status.getValue() == null) {
                formDialogController.setError("Status is required.");
                formDialogController.setLoading(false);
                return;
            }
            appointment.setStatus(status.getValue());
            refreshTable();
            formDialogController.close();
            toastSuccess("Appointment status updated.");
            try {
                String dbValue = AppointmentStatus.fromDbValue(status.getValue()).getDbValue();
                appointmentService.update(new UpdateAppointmentDTO(
                        appointment.getAppointmentId(), null, dbValue, null));
                refreshTable();
                formDialogController.close();
                toastSuccess("Appointment status updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to update status: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Status", "fas-info-circle", status);
    }

    private static String statusLabel(String status) {
        try {
            return AppointmentStatus.fromDbValue(status).getLabel();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }
}
+ Thought: 5.4s
Now the InvoicePageController:
← Edit src\main\java\hospital\management\pages\finance\InvoicePageController.java
import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.finance.InvoiceDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.dto.finance.CreateInvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceSummaryDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.finance.InvoiceServiceImpl;
import hospital.management.backend.service.finance.interfaces.InvoiceService;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class InvoicePageController extends BasePageController implements QuickAddCapable {

    private static final String STATUS_PAID = "paid";

    private final InvoiceService invoiceService = new InvoiceServiceImpl(new InvoiceDAOImpl(), new PatientDAOImpl());
    private final PatientServiceImpl patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();
    @FXML private Button newInvoiceBtn;
    @FXML private Button exportCsvBtn;
    @FXML private Button printReportBtn;

    private final List<Invoice> invoices = new ArrayList<>();
    private final List<InvoiceDTO> invoices = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.BILLING);

        totalRevenueLabel.setText("$0.00");
        paidLabel.setText("$0.00");
        pendingLabel.setText("$0.00");

        newInvoiceBtn.setOnAction(e -> openInvoiceDialog(null));
        newInvoiceBtn.setOnAction(e -> openInvoiceDialog());
        exportCsvBtn.setOnAction(e -> toast("Export not yet implemented.", NotificationType.INFO));
        printReportBtn.setOnAction(e -> toast("Print not yet implemented.", NotificationType.INFO));

        invoiceTableController.setRowActions(this::openInvoiceDialog, this::confirmDeleteInvoice, this::viewInvoiceDetail);
        invoiceTableController.setOnChangeStatus(this::openInvoiceStatusDialog);
        invoiceTableController.setRowActions(
                invoice -> toast("Invoices can't be edited after issuance.", NotificationType.INFO),
                this::confirmDeleteInvoice, this::viewInvoiceDetail);
        invoiceTableController.setOnChangeStatus(this::markInvoicePaid);

        refreshTable();
    }

    private void refreshTable() {
        invoiceTableController.setItems(invoices);
        try {
            invoices.clear();
            List<InvoiceSummaryDTO> summaries =
                    invoiceService.findAll(CursorPagination.firstPage(500)).getItems();
            for (InvoiceSummaryDTO summary : summaries) {
                invoices.add(invoiceService.findById(summary.getInvoiceId()));
            }
            invoiceTableController.setItems(invoices);
            updateSummaryLabels();
        } catch (Exception e) {
            toastError("Failed to load invoices: " + e.getMessage());
        }
    }

    private void viewInvoiceDetail(Invoice invoice) {
    private void updateSummaryLabels() {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (InvoiceDTO invoice : invoices) {
            BigDecimal amount = invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount();
            total = total.add(amount);
            if (STATUS_PAID.equalsIgnoreCase(invoice.getPaymentStatus())) {
                paid = paid.add(amount);
            }
        }
        totalRevenueLabel.setText("$" + total.toPlainString());
        paidLabel.setText("$" + paid.toPlainString());
        pendingLabel.setText("$" + total.subtract(paid).toPlainString());
    }

    private void viewInvoiceDetail(InvoiceDTO invoice) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Patient", entityLookupService.patientLabel(invoice.getPatientId()));
            fields.put("Appointment", entityLookupService.appointmentLabel(invoice.getAppointmentId()));
        fields.put("Issued At", invoice.getIssuedAt() == null ? null : invoice.getIssuedAt().toString());
        detailViewController.show("Invoice Details", "fas-file-invoice-dollar", fields);
    }

    private void confirmDeleteInvoice(Invoice invoice) {
    private void confirmDeleteInvoice(InvoiceDTO invoice) {
        confirm("Delete Invoice",
                "Are you sure you want to delete invoice " + invoice.getInvoiceId() + "? This cannot be undone.",
                () -> {
                    invoices.remove(invoice);
                    refreshTable();
                    toastSuccess("Invoice deleted.");
                    try {
                        invoiceService.delete(invoice.getInvoiceId());
                        refreshTable();
                        toastSuccess("Invoice deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete invoice: " + e.getMessage());
                    }
                });
    }

    /** The backend's only payment-status transition is to mark an unpaid invoice as paid. */
    private void markInvoicePaid(InvoiceDTO invoice) {
        if (STATUS_PAID.equalsIgnoreCase(invoice.getPaymentStatus())) {
            toast("This invoice is already paid.", NotificationType.INFO);
            return;
        }
        confirm("Mark Invoice Paid",
                "Are you sure you want to mark invoice " + invoice.getInvoiceId() + " as paid?",
                () -> {
                    try {
                        invoiceService.markPaid(invoice.getInvoiceId());
                        refreshTable();
                        toastSuccess("Invoice marked as paid.");
                    } catch (Exception e) {
                        toastError("Failed to update invoice status: " + e.getMessage());
                    }
                });
    }

    @Override
    public void openAddDialog() {
        openInvoiceDialog(null);
        openInvoiceDialog();
    }

    /** Opens the shared form dialog in Add mode (invoice == null) or Update mode. */
    private void openInvoiceDialog(Invoice invoice) {
        boolean addMode = invoice == null;

    /** Opens the shared form dialog to generate a new invoice. */
    private void openInvoiceDialog() {
        LoadingIdComboBox patientIdField     = new LoadingIdComboBox();
        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        EntityIdComboBox patientId     = patientIdField.getComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();

        List<Control> otherFields = List.of(totalAmount);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            totalAmount.setText(invoice.getTotalAmount() == null ? "" : invoice.getTotalAmount().toPlainString());
        }

        formDialogController.open(addMode ? "Add Invoice" : "Update Invoice", "fas-file-invoice-dollar", addMode, v -> {
        formDialogController.open("Add Invoice", "fas-file-invoice-dollar", true, v -> {
            String pid = patientId.getSelectedId();
            String aid = appointmentId.getSelectedId();
            String amountText = totalAmount.getText() == null ? "" : totalAmount.getText().trim();

                formDialogController.setLoading(false);
                return;
            }

            Invoice target = addMode ? new Invoice() : invoice;
            if (addMode) {
                target.setInvoiceId(UUID.randomUUID().toString());
                target.setPaymentStatus("Pending");
            try {
                invoiceService.generate(new CreateInvoiceDTO(aid, pid, amount));
                refreshTable();
                formDialogController.close();
                toastSuccess("Invoice added.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save invoice: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
            target.setPatientId(pid);
            target.setAppointmentId(aid);
            target.setTotalAmount(amount);
            if (addMode) {
                target.setIssuedAt(LocalDateTime.now());
            } else {
                target.setUpdatedAt(LocalDateTime.now());
            }

            if (addMode) invoices.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Invoice added." : "Invoice updated.");
        });

        formDialogController.addField("Patient", "fas-user", patientIdField);
        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Total Amount", "fas-dollar-sign", totalAmount);

        loadInvoiceDropdowns(patientIdField, appointmentIdField, otherFields, addMode ? null : invoice);
        loadInvoiceDropdowns(patientIdField, appointmentIdField, otherFields);
    }

    /** Loads the patient/appointment dropdown options asynchronously, showing each dropdown's own
     *  spinner while its data is in flight and keeping the rest of the form disabled until
     *  both have finished loading. */
    private void loadInvoiceDropdowns(LoadingIdComboBox patientIdField, LoadingIdComboBox appointmentIdField,
                                       List<Control> otherFields, Invoice existing) {
                                       List<Control> otherFields) {
        EntityIdComboBox patientId = patientIdField.getComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();

        patientIdField.setLoading(true);
            () -> patientService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                patientId.setOptions(items.stream()
                        .map(p -> new EntityIdComboBox.Option(p.getPatientId(), p.getFullName())).toList());
                if (existing != null) patientId.selectById(existing.getPatientId());
                patientIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                appointmentId.setOptions(items.stream()
                        .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                                a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                        .toList());
                if (existing != null) appointmentId.selectById(existing.getAppointmentId());
                appointmentIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                toastError("Failed to load appointments: " + ex.getMessage());
                onOneLoaded.run();
            });
    }

    /** Minimal single-field dialog for changing an existing invoice's payment status, kept out of the main Add/Edit form. */
    private void openInvoiceStatusDialog(Invoice invoice) {
        ComboBox<String> paymentStatus = new ComboBox<>();
        paymentStatus.getStyleClass().add("form-combo");
        paymentStatus.getItems().addAll("Pending", "Paid", "Overdue", "Cancelled");
        paymentStatus.setValue(invoice.getPaymentStatus());

        formDialogController.open("Change Payment Status", "fas-info-circle", false, v -> {
            if (paymentStatus.getValue() == null) {
                formDialogController.setError("Payment status is required.");
                formDialogController.setLoading(false);
                return;
            }
            invoice.setPaymentStatus(paymentStatus.getValue());
            invoice.setUpdatedAt(LocalDateTime.now());
            refreshTable();
            formDialogController.close();
            toastSuccess("Invoice payment status updated.");
        });

        formDialogController.addField("Payment Status", "fas-info-circle", paymentStatus);
    }
}
+ Thought: 6.9s
Let me check EntityIdComboBox and LoadingIdComboBox structure before writing the prescription items editor.
→Read src\main\java\hospital\management\pages\components\shared\search\EntityIdComboBox.java 
→Read src\main\java\hospital\management\pages\components\shared\search\LoadingIdComboBox.java 
+ Thought: 601ms
Now the PrescriptionsController with an items editor:
# Wrote src\main\java\hospital\management\pages\pharmacy\PrescriptionsController.java
package hospital.management.pages.pharmacy;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicalInventoryDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicationDAOImpl;
import hospital.management.backend.dao.pharmacy.PrescriptionDAOImpl;
import hospital.management.backend.dao.pharmacy.PrescriptionItemDAOImpl;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionDTO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionItemDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.service.pharmacy.PrescriptionServiceImpl;
import hospital.management.backend.service.pharmacy.interfaces.PrescriptionService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.backend.service.pharmacy.PharmacyServiceImpl;
import hospital.management.pages.components.pharmacy.PrescriptionTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PrescriptionsController extends BasePageController {

    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final PrescriptionService prescriptionService =
            new PrescriptionServiceImpl(new PrescriptionDAOImpl(), new PrescriptionItemDAOImpl());
    private final PharmacyServiceImpl pharmacyService = new PharmacyServiceImpl(
        new MedicationDAOImpl(), new MedicalInventoryDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private PrescriptionTableController prescriptionTableController;

    @FXML private TextField  searchField;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button     newPrescriptionBtn;

    private final List<PrescriptionDTO> prescriptions = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PRESCRIPTIONS);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        newPrescriptionBtn.setOnAction(e -> openPrescriptionDialog());
        prescriptionTableController.setRowActions(
                p -> toast("Prescriptions can't be edited once issued.", NotificationType.INFO),
                this::confirmDeletePrescription, this::viewPrescriptionDetail);

        refreshTable();
    }

    private void applyFilter() {
        prescriptionTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        try {
            prescriptions.clear();
            List<AppointmentSummaryDTO> appointments =
                    appointmentService.findAll(CursorPagination.firstPage(500)).getItems();
            for (AppointmentSummaryDTO appointment : appointments) {
                try {
                    prescriptions.add(prescriptionService.findByAppointment(appointment.getAppointmentId()));
                } catch (ResourceNotFoundException ignored) {
                }
            }
            prescriptionTableController.setItems(prescriptions);
        } catch (Exception e) {
            toastError("Failed to load prescriptions: " + e.getMessage());
        }
    }

    private void viewPrescriptionDetail(PrescriptionDTO prescription) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Appointment", entityLookupService.appointmentLabel(prescription.getAppointmentId()));
        } catch (Exception ex) {
            toastError("Failed to resolve appointment: " + ex.getMessage());
        }
        fields.put("Date Issued", prescription.getDateIssued() == null ? null : prescription.getDateIssued().toString());
        if (prescription.getItems() != null && !prescription.getItems().isEmpty()) {
            StringBuilder items = new StringBuilder();
            for (hospital.management.backend.dto.pharmacy.PrescriptionItemDTO item : prescription.getItems()) {
                items.append("• ").append(item.getDosage() == null ? "" : item.getDosage())
                        .append(" × ").append(item.getQuantity() == null ? "" : item.getQuantity())
                        .append(" (").append(item.getInstructions() == null ? "" : item.getInstructions())
                        .append(")\n");
            }
            fields.put("Items", items.toString());
        }
        detailViewController.show("Prescription Details", "fas-prescription", fields);
    }

    private void confirmDeletePrescription(PrescriptionDTO prescription) {
        confirm("Delete Prescription",
                "Are you sure you want to delete prescription " + prescription.getPrescriptionId() + "? This cannot be undone.",
                () -> {
                    try {
                        prescriptionService.delete(prescription.getPrescriptionId());
                        refreshTable();
                        toastSuccess("Prescription deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete prescription: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog to issue a new prescription (with at least one line item). */
    private void openPrescriptionDialog() {
        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        DatePicker dateIssued = new DatePicker();

        LoadingIdComboBox medicationField = new LoadingIdComboBox();
        EntityIdComboBox medicationId = medicationField.getComboBox();
        TextField dosage        = new TextField();
        TextField quantity      = new TextField();
        TextField instructions  = new TextField();
        dosage.setPromptText("Dosage");
        quantity.setPromptText("Qty");
        instructions.setPromptText("Instructions");
        Button addItemBtn    = new Button("Add Item");
        Button removeItemBtn = new Button("Remove Selected");
        ListView<String> itemsList = new ListView<>();
        itemsList.setPrefHeight(110);

        appointmentId.getStyleClass().add("form-combo");
        dateIssued.getStyleClass().add("form-date-picker");
        List.of(dosage, quantity, instructions).forEach(f -> f.getStyleClass().add("form-input"));
        medicationId.getStyleClass().add("form-combo");
        addItemBtn.getStyleClass().add("secondary-button");
        removeItemBtn.getStyleClass().add("secondary-button");

        List<Control> otherFields = List.of(dateIssued);
        otherFields.forEach(f -> f.setDisable(true));
        List<Control> itemEditorFields = List.of(dosage, quantity, instructions, addItemBtn, removeItemBtn);
        itemEditorFields.forEach(f -> f.setDisable(true));

        List<CreatePrescriptionItemDTO> draftItems = new ArrayList<>();

        addItemBtn.setOnAction(e -> {
            String medId = medicationId.getSelectedId();
            String medLabel = medicationId.getValue() == null ? "" : medicationId.getValue().label();
            String qtyText = quantity.getText() == null ? "" : quantity.getText().trim();
            if (medId == null) {
                toastError("Select a medication first.");
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(qtyText);
            } catch (NumberFormatException ex) {
                toastError("Quantity must be a whole number.");
                return;
            }
            if (qty <= 0) {
                toastError("Quantity must be greater than zero.");
                return;
            }
            draftItems.add(new CreatePrescriptionItemDTO(medId, dosage.getText(), qty, instructions.getText()));
            itemsList.getItems().add(medLabel + " | " + dosage.getText() + " | qty " + qty
                    + (instructions.getText() == null || instructions.getText().isBlank() ? "" : " | " + instructions.getText()));
            dosage.clear();
            quantity.clear();
            instructions.clear();
        });

        removeItemBtn.setOnAction(e -> {
            int index = itemsList.getSelectionModel().getSelectedIndex();
            if (index < 0) {
                toastError("Select an item in the list to remove it.");
                return;
            }
            itemsList.getItems().remove(index);
            draftItems.remove(index);
        });

        VBox itemsBox = new VBox(6);
        HBox itemInputRow = new HBox(6, medicationField, dosage, quantity, instructions, addItemBtn);
        itemInputRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        itemsBox.getChildren().addAll(itemInputRow, itemsList, removeItemBtn);

        formDialogController.open("New Prescription", "fas-prescription", true, v -> {
            String apptId = appointmentId.getSelectedId();
            if (apptId == null || dateIssued.getValue() == null) {
                formDialogController.setError("Appointment and date issued are required.");
                formDialogController.setLoading(false);
                return;
            }
            if (draftItems.isEmpty()) {
                formDialogController.setError("A prescription must include at least one medication item.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                prescriptionService.issue(new CreatePrescriptionDTO(apptId, dateIssued.getValue(), List.copyOf(draftItems)));
                refreshTable();
                formDialogController.close();
                toastSuccess("Prescription issued.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to issue prescription: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Date Issued", "fas-calendar", dateIssued);
        formDialogController.addField("Medication", "fas-pills", medicationField);
        formDialogController.addRow(itemsBox);

        loadPrescriptionDropdown(appointmentIdField, medicationField, otherFields, itemEditorFields);
    }

    /** Loads the appointment and medication dropdown options asynchronously, showing each
     *  dropdown's own spinner while its data is in flight and keeping the rest of the form
     *  disabled until both have finished loading. */
    private void loadPrescriptionDropdown(LoadingIdComboBox appointmentIdField, LoadingIdComboBox medicationField,
                                          List<Control> otherFields, List<Control> itemEditorFields) {
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        EntityIdComboBox medicationId = medicationField.getComboBox();

        appointmentIdField.setLoading(true);
        medicationField.setLoading(true);
        formDialogController.setLoading(true);

        java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger(2);
        Runnable onOneLoaded = () -> {
            if (pending.decrementAndGet() == 0) {
                otherFields.forEach(f -> f.setDisable(false));
                itemEditorFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            }
        };

        AsyncJobRunner.submit(
            () -> appointmentService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                appointmentId.setOptions(items.stream()
                        .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                                a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                        .toList());
                appointmentIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                appointmentIdField.setLoading(false);
                toastError("Failed to load appointments: " + ex.getMessage());
                onOneLoaded.run();
            });

        AsyncJobRunner.submit(
            pharmacyService::findAllMedications,
            items -> {
                medicationId.setOptions(items.stream()
                        .map(m -> new EntityIdComboBox.Option(m.getMedicationId(), m.getName())).toList());
                medicationField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                medicationField.setLoading(false);
                toastError("Failed to load medications: " + ex.getMessage());
                onOneLoaded.run();
            });
    }
}

+ Thought: 212ms
Let me verify PrescriptionItemDTO getters:
✱Grep "public (String|Integer|String) get" in src\main\java\hospital\management\backend\dto\pharmacy\PrescriptionItemDTO.java (31 matches)
+ Thought: 5.7s
Now the LabOrdersController:
# Wrote src\main\java\hospital\management\pages\lab\LabOrdersController.java
package hospital.management.pages.lab;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.lab.LabOrderDAOImpl;
import hospital.management.backend.dao.lab.LabResultDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.lab.CreateLabOrderDTO;
import hospital.management.backend.dto.lab.CreateLabResultDTO;
import hospital.management.backend.dto.lab.LabOrderDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.lab.LabServiceImpl;
import hospital.management.backend.service.lab.interfaces.LabService;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.lab.LabOrderTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LabOrdersController extends BasePageController {

    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final DoctorServiceImpl doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
    private final LabService labService = new LabServiceImpl(new LabOrderDAOImpl(), new LabResultDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private LabOrderTableController labOrderTableController;

    @FXML private TextField    searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button       newOrderBtn;

    private final List<LabOrderDTO> labOrders = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.LAB_ORDERS);

        statusFilter.getItems().addAll("All", "ORDERED", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        statusFilter.setValue("All");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());

        newOrderBtn.setOnAction(e -> openLabOrderDialog());
        labOrderTableController.setRowActions(
                o -> toast("Lab orders can't be edited once placed.", NotificationType.INFO),
                this::confirmDeleteLabOrder, this::viewLabOrderDetail);
        labOrderTableController.setOnChangeStatus(this::openRecordResultDialog);

        refreshTable();
    }

    private void applyFilter() {
        String selected = statusFilter.getValue();
        List<LabOrderDTO> visible = labOrders;
        if (selected != null && !"All".equals(selected)) {
            visible = labOrders.stream()
                    .filter(o -> selected.equalsIgnoreCase(o.getStatus()))
                    .toList();
        }
        labOrderTableController.setItems(visible);
        labOrderTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        try {
            labOrders.clear();
            List<AppointmentSummaryDTO> appointments =
                    appointmentService.findAll(CursorPagination.firstPage(500)).getItems();
            for (AppointmentSummaryDTO appointment : appointments) {
                labOrders.addAll(labService.findOrdersByAppointment(appointment.getAppointmentId()));
            }
            labOrders.sort(Comparator.comparing(LabOrderDTO::getOrderedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            applyFilter();
        } catch (Exception e) {
            toastError("Failed to load lab orders: " + e.getMessage());
        }
    }

    private void viewLabOrderDetail(LabOrderDTO labOrder) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Appointment", entityLookupService.appointmentLabel(labOrder.getAppointmentId()));
            fields.put("Doctor", entityLookupService.doctorLabel(labOrder.getDoctorId()));
        } catch (Exception ex) {
            toastError("Failed to resolve lab order details: " + ex.getMessage());
        }
        fields.put("Test Name", labOrder.getTestName());
        fields.put("Status", labOrder.getStatus());
        fields.put("Ordered At", labOrder.getOrderedAt() == null ? null : labOrder.getOrderedAt().toString());
        detailViewController.show("Lab Order Details", "fas-flask", fields);
    }

    private void confirmDeleteLabOrder(LabOrderDTO labOrder) {
        confirm("Delete Lab Order",
                "Are you sure you want to delete the order for " + labOrder.getTestName() + "? This cannot be undone.",
                () -> {
                    try {
                        labService.deleteOrder(labOrder.getLabOrderId());
                        refreshTable();
                        toastSuccess("Lab order deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete lab order: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog to order a new lab test. */
    private void openLabOrderDialog() {
        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        LoadingIdComboBox doctorIdField      = new LoadingIdComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        EntityIdComboBox doctorId      = doctorIdField.getComboBox();
        TextField testName      = new TextField();

        testName.getStyleClass().add("form-input");
        List.of(appointmentId, doctorId).forEach(f -> f.getStyleClass().add("form-combo"));

        List<Control> otherFields = List.of(testName);
        otherFields.forEach(f -> f.setDisable(true));

        formDialogController.open("Add Lab Order", "fas-flask", true, v -> {
            String apptId = appointmentId.getSelectedId();
            String docId  = doctorId.getSelectedId();
            String test   = testName.getText() == null ? "" : testName.getText().trim();
            if (apptId == null || docId == null || test.isEmpty()) {
                formDialogController.setError("Appointment, doctor and test name are required.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                labService.orderTest(new CreateLabOrderDTO(apptId, docId, test));
                refreshTable();
                formDialogController.close();
                toastSuccess("Lab order added.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to add lab order: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Doctor", "fas-user-md", doctorIdField);
        formDialogController.addField("Test Name", "fas-vial", testName);

        loadLabOrderDropdowns(appointmentIdField, doctorIdField, otherFields);
    }

    /** Loads the appointment/doctor dropdown options asynchronously, showing each dropdown's own
     *  spinner while its data is in flight and keeping the rest of the form disabled until
     *  both have finished loading. */
    private void loadLabOrderDropdowns(LoadingIdComboBox appointmentIdField, LoadingIdComboBox doctorIdField,
                                        List<Control> otherFields) {
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        EntityIdComboBox doctorId = doctorIdField.getComboBox();

        appointmentIdField.setLoading(true);
        doctorIdField.setLoading(true);
        formDialogController.setLoading(true);

        AtomicInteger pending = new AtomicInteger(2);
        Runnable onOneLoaded = () -> {
            if (pending.decrementAndGet() == 0) {
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            }
        };

        AsyncJobRunner.submit(
            () -> appointmentService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                appointmentId.setOptions(items.stream()
                        .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                                a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                        .toList());
                appointmentIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                appointmentIdField.setLoading(false);
                toastError("Failed to load appointments: " + ex.getMessage());
                onOneLoaded.run();
            });

        AsyncJobRunner.submit(
            () -> doctorService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                doctorId.setOptions(items.stream()
                        .map(d -> new EntityIdComboBox.Option(d.getDoctorId(), d.getFullName())).toList());
                doctorIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                doctorIdField.setLoading(false);
                toastError("Failed to load doctors: " + ex.getMessage());
                onOneLoaded.run();
            });
    }

    /** Recording a result is the only status transition the backend supports for a lab order —
     *  it flips the order to Completed in the same transaction that persists the result. */
    private void openRecordResultDialog(LabOrderDTO labOrder) {
        if ("completed".equalsIgnoreCase(labOrder.getStatus())) {
            toast("This lab order already has a result.", NotificationType.INFO);
            return;
        }

        TextField resultValue = new TextField();
        TextField unit        = new TextField();
        TextField referenceRange = new TextField();
        ComboBox<String> abnormal = new ComboBox<>();

        resultValue.getStyleClass().add("form-input");
        unit.getStyleClass().add("form-input");
        referenceRange.getStyleClass().add("form-input");
        abnormal.getStyleClass().add("form-combo");
        abnormal.getItems().addAll("Normal", "Abnormal");
        abnormal.setValue("Normal");

        formDialogController.open("Record Result", "fas-flask", false, v -> {
            String value = resultValue.getText() == null ? "" : resultValue.getText().trim();
            if (value.isEmpty()) {
                formDialogController.setError("Result value is required.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                labService.recordResult(new CreateLabResultDTO(
                        labOrder.getLabOrderId(), value, unit.getText(), referenceRange.getText(),
                        "Abnormal".equals(abnormal.getValue()), LocalDateTime.now()));
                refreshTable();
                formDialogController.close();
                toastSuccess("Lab result recorded — order completed.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to record result: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Result Value", "fas-chart-line", resultValue);
        formDialogController.addField("Unit", "fas-flask", unit);
        formDialogController.addField("Reference Range", "fas-arrows-alt-h", referenceRange);
        formDialogController.addField("Result", "fas-exclamation-circle", abnormal);
    }
}

+ Thought: 4.1s
Now the PharmacyController full integration:
# Wrote src\main\java\hospital\management\pages\pharmacy\PharmacyController.java
package hospital.management.pages.pharmacy;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicalInventoryDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicationDAOImpl;
import hospital.management.backend.dao.pharmacy.PrescriptionDAOImpl;
import hospital.management.backend.dao.pharmacy.PrescriptionItemDAOImpl;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.pharmacy.CreateMedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.MedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.MedicationDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.pharmacy.PharmacyServiceImpl;
import hospital.management.backend.service.pharmacy.PrescriptionServiceImpl;
import hospital.management.backend.service.pharmacy.interfaces.PrescriptionService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.pharmacy.MedicalInventoryTableController;
import hospital.management.pages.components.pharmacy.PrescriptionTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PharmacyController extends BasePageController {

    private final PharmacyServiceImpl pharmacyService = new PharmacyServiceImpl(
        new MedicationDAOImpl(), new MedicalInventoryDAOImpl());
    private final PrescriptionService prescriptionService =
            new PrescriptionServiceImpl(new PrescriptionDAOImpl(), new PrescriptionItemDAOImpl());
    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());

    @FXML private TabPane pharmacyTabs;

    // Inventory tab
    @FXML private TextField inventorySearchField;
    @FXML private Button    addMedBtn;
    @FXML private MedicalInventoryTableController inventoryTableController;

    // Low stock tab (same underlying data, filtered)
    @FXML private MedicalInventoryTableController lowStockTableController;

    // Pending prescriptions tab
    @FXML private PrescriptionTableController pendingPrescriptionsTableController;

    private final List<MedicalInventoryDTO> inventory = new ArrayList<>();
    private final List<PrescriptionDTO> pendingPrescriptions = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PHARMACY);

        addMedBtn.setOnAction(e -> openInventoryDialog(null));
        inventorySearchField.textProperty().addListener((obs, o, n) -> applyFilter());

        inventoryTableController.setRowActions(this::openInventoryDialog, this::confirmDeleteInventory, this::viewInventoryDetail);
        lowStockTableController.setRowActions(this::openInventoryDialog, this::confirmDeleteInventory, this::viewInventoryDetail);

        refreshInventoryTables();
        refreshPendingPrescriptions();
    }

    private void applyFilter() {
        inventoryTableController.filter(inventorySearchField.getText());
    }

    private void refreshInventoryTables() {
        try {
            inventory.clear();
            List<MedicationDTO> medications = pharmacyService.findAllMedications();
            for (MedicationDTO medication : medications) {
                inventory.addAll(pharmacyService.findStockByMedication(medication.getMedicationId()));
            }
            inventoryTableController.setItems(inventory);
            lowStockTableController.setItems(pharmacyService.findLowStock());
        } catch (Exception e) {
            toastError("Failed to load inventory: " + e.getMessage());
        }
    }

    private void refreshPendingPrescriptions() {
        try {
            pendingPrescriptions.clear();
            List<AppointmentSummaryDTO> appointments =
                    appointmentService.findAll(CursorPagination.firstPage(500)).getItems();
            for (AppointmentSummaryDTO appointment : appointments) {
                try {
                    pendingPrescriptions.add(prescriptionService.findByAppointment(appointment.getAppointmentId()));
                } catch (ResourceNotFoundException ignored) {
                }
            }
            pendingPrescriptionsTableController.setItems(pendingPrescriptions);
        } catch (Exception e) {
            toastError("Failed to load prescriptions: " + e.getMessage());
        }
    }

    private void viewInventoryDetail(MedicalInventoryDTO item) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Medication", pharmacyService.findMedicationById(item.getMedicationId()).getName());
        } catch (Exception ex) {
            fields.put("Medication", "Unknown");
        }
        fields.put("Batch Number", item.getBatchNumber());
        fields.put("Expiry Date", item.getExpiryDate() == null ? null : item.getExpiryDate().toString());
        fields.put("Quantity In Stock", item.getQuantityInStock() == null ? null : String.valueOf(item.getQuantityInStock()));
        fields.put("Reorder Level", item.getReorderLevel() == null ? null : String.valueOf(item.getReorderLevel()));
        fields.put("Supplier", item.getSupplier());
        detailViewController.show("Medication Details", "fas-pills", fields);
    }

    private void confirmDeleteInventory(MedicalInventoryDTO item) {
        toast("Inventory batches can't be deleted — update the batch instead.", NotificationType.INFO);
    }

    /** Opens the shared form dialog in Add mode (item == null) or Update mode. */
    private void openInventoryDialog(MedicalInventoryDTO item) {
        boolean addMode = item == null;

        LoadingIdComboBox medicationIdField = new LoadingIdComboBox();
        EntityIdComboBox medicationId = medicationIdField.getComboBox();
        TextField batchNumber     = new TextField();
        DatePicker expiryDate     = new DatePicker();
        TextField quantityInStock = new TextField();
        TextField reorderLevel    = new TextField();
        TextField supplier        = new TextField();

        List.of(batchNumber, quantityInStock, reorderLevel, supplier)
                .forEach(f -> f.getStyleClass().add("form-input"));
        medicationId.getStyleClass().add("form-combo");
        expiryDate.getStyleClass().add("form-date-picker");

        List<Control> otherFields = List.of(batchNumber, expiryDate, quantityInStock, reorderLevel, supplier);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            batchNumber.setText(item.getBatchNumber());
            expiryDate.setValue(item.getExpiryDate());
            quantityInStock.setText(item.getQuantityInStock() != null ? String.valueOf(item.getQuantityInStock()) : "");
            reorderLevel.setText(item.getReorderLevel() != null ? String.valueOf(item.getReorderLevel()) : "");
            supplier.setText(item.getSupplier());
        }

        formDialogController.open(addMode ? "Add Medication" : "Update Medication", "fas-pills", addMode, v -> {
            String medId = medicationId.getSelectedId();
            String batch = batchNumber.getText() == null ? "" : batchNumber.getText().trim();

            if (medId == null || batch.isEmpty() || expiryDate.getValue() == null) {
                formDialogController.setError("Medication, batch number and expiry date are required.");
                formDialogController.setLoading(false);
                return;
            }

            int qty;
            int reorder;
            try {
                qty = Integer.parseInt(quantityInStock.getText().trim());
            } catch (NumberFormatException ex) {
                formDialogController.setError("Quantity in stock must be a whole number.");
                formDialogController.setLoading(false);
                return;
            }
            try {
                reorder = Integer.parseInt(reorderLevel.getText().trim());
            } catch (NumberFormatException ex) {
                formDialogController.setError("Reorder level must be a whole number.");
                formDialogController.setLoading(false);
                return;
            }

            CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(
                    medId, batch, expiryDate.getValue(), qty, reorder, supplier.getText());

            try {
                if (addMode) {
                    pharmacyService.addStock(dto);
                } else {
                    pharmacyService.updateStock(item.getInventoryId(), dto);
                }
                refreshInventoryTables();
                formDialogController.close();
                toastSuccess(addMode ? "Medication added." : "Medication updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save inventory: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Medication", "fas-pills", medicationIdField);
        formDialogController.addField("Batch Number", "fas-barcode", batchNumber);
        formDialogController.addField("Expiry Date", "fas-calendar", expiryDate);
        formDialogController.addField("Quantity In Stock", "fas-boxes", quantityInStock);
        formDialogController.addField("Reorder Level", "fas-exclamation-triangle", reorderLevel);
        formDialogController.addField("Supplier", "fas-truck", supplier);

        loadMedicationDropdown(medicationIdField, otherFields, addMode ? null : item);
    }

    /** Loads the medication dropdown options asynchronously, showing its own spinner while
     *  data is in flight and keeping the rest of the form disabled until it finishes loading. */
    private void loadMedicationDropdown(LoadingIdComboBox medicationIdField, List<Control> otherFields, MedicalInventoryDTO existing) {
        EntityIdComboBox medicationId = medicationIdField.getComboBox();

        medicationIdField.setLoading(true);
        formDialogController.setLoading(true);

        AsyncJobRunner.submit(
            pharmacyService::findAllMedications,
            items -> {
                medicationId.setOptions(items.stream()
                        .map(m -> new EntityIdComboBox.Option(m.getMedicationId(), m.getName()))
                        .toList());
                if (existing != null) medicationId.selectById(existing.getMedicationId());
                medicationIdField.setLoading(false);
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            },
            ex -> {
                medicationIdField.setLoading(false);
                toastError("Failed to load medications: " + ex.getMessage());
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            });
    }
}

+ Thought: 7.8s
Now PatientDetailController — targeted edits. First the imports:
← Edit src\main\java\hospital\management\pages\patient\PatientDetailController.java
package hospital.management.pages.patient;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.clinical.MedicalRecordDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.finance.InvoiceDAOImpl;
import hospital.management.backend.dao.lab.LabOrderDAOImpl;
import hospital.management.backend.dao.lab.LabResultDAOImpl;
import hospital.management.backend.dao.patient.PatientAllergyDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.patient.VitalSignDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicalInventoryDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicationDAOImpl;
import hospital.management.backend.dao.pharmacy.PrescriptionDAOImpl;
import hospital.management.backend.dao.pharmacy.PrescriptionItemDAOImpl;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.clinical.CreateMedicalRecordDTO;
import hospital.management.backend.dto.clinical.MedicalRecordDTO;
import hospital.management.backend.dto.finance.InvoiceDTO;
import hospital.management.backend.dto.lab.LabOrderDTO;
import hospital.management.backend.dto.patient.CreatePatientAllergyDTO;
import hospital.management.backend.dto.patient.CreateVitalSignDTO;
import hospital.management.backend.dto.patient.PatientAllergyDTO;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.patient.UpdatePatientDTO;
import hospital.management.backend.dto.patient.VitalSignDTO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionDTO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionItemDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.model.lab.LabOrder;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.MedicalRecord;
import hospital.management.backend.model.pharmacy.Prescription;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.clinical.MedicalRecordServiceImpl;
import hospital.management.backend.service.clinical.interfaces.MedicalRecordService;
import hospital.management.backend.service.finance.InvoiceServiceImpl;
import hospital.management.backend.service.finance.interfaces.InvoiceService;
import hospital.management.backend.service.lab.LabServiceImpl;
import hospital.management.backend.service.lab.interfaces.LabService;
import hospital.management.backend.service.patient.AllergyServiceImpl;
import hospital.management.backend.service.patient.interfaces.AllergyService;
import hospital.management.backend.service.patient.interfaces.PatientService;
import hospital.management.backend.service.patient.interfaces.VitalSignService;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.service.patient.VitalSignServiceImpl;
import hospital.management.backend.service.pharmacy.PharmacyServiceImpl;
import hospital.management.backend.service.pharmacy.PrescriptionServiceImpl;
import hospital.management.backend.service.pharmacy.interfaces.PrescriptionService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.clinical.AppointmentTableController;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read-heavy drill-down view for a single patient. Reached by calling
 * {@link #loadPatient(PatientDTO)} directly on the loaded controller instance
Now the service fields and list types:
← Edit src\main\java\hospital\management\pages\patient\PatientDetailController.java
    new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
private final PatientService patientService = new PatientServiceImpl(new PatientDAOImpl());
private final VitalSignService vitalSignService = new VitalSignServiceImpl(new VitalSignDAOImpl());
private final AllergyService allergyService = new AllergyServiceImpl(new PatientAllergyDAOImpl());
private final MedicalRecordService medicalRecordService = new MedicalRecordServiceImpl(new MedicalRecordDAOImpl());
private final PrescriptionService prescriptionService =
        new PrescriptionServiceImpl(new PrescriptionDAOImpl(), new PrescriptionItemDAOImpl());
private final LabService labService = new LabServiceImpl(new LabOrderDAOImpl(), new LabResultDAOImpl());
private final InvoiceService invoiceService = new InvoiceServiceImpl(new InvoiceDAOImpl(), new PatientDAOImpl());
private final PharmacyServiceImpl pharmacyService = new PharmacyServiceImpl(
    new MedicationDAOImpl(), new MedicalInventoryDAOImpl());

// Header
@FXML private Label  patientNameLabel;
@FXML private Button backBtn;
← Edit src\main\java\hospital\management\pages\patient\PatientDetailController.java

private PatientDTO currentPatient;

private final List<VitalSignDTO>      vitals         = new ArrayList<>();
private final List<MedicalRecord>     medicalRecords = new ArrayList<>();
private final List<Appointment>       appointments   = new ArrayList<>();
private final List<Prescription>      prescriptions  = new ArrayList<>();
private final List<LabOrder>          labOrders      = new ArrayList<>();
private final List<MedicalRecordDTO>  medicalRecords = new ArrayList<>();
private final List<AppointmentDTO>    appointments   = new ArrayList<>();
private final List<PrescriptionDTO>   prescriptions  = new ArrayList<>();
private final List<LabOrderDTO>       labOrders      = new ArrayList<>();
private final List<PatientAllergyDTO> allergies      = new ArrayList<>();
private final List<Invoice>           invoices       = new ArrayList<>();
private final List<InvoiceDTO>        invoices       = new ArrayList<>();

public void initialize() {
    if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PATIENTS);

Now loadPatient and the refresh methods:
← Edit src\main\java\hospital\management\pages\patient\PatientDetailController.java
    addressLabel.setText("Address: " + (patient.getAddress() != null ? patient.getAddress() : "—"));
    patientNameLabel.setText(patient.getFullName());
    refreshVitals();
    refreshAllergies();
    refreshAppointments();
    refreshInvoices();
}

private void refreshVitals() {
    if (currentPatient == null) return;
        toastError("Failed to load allergies: " + e.getMessage());
    }
}

private void refreshAppointments() {
    if (currentPatient == null) return;
    try {
        appointments.clear();
        appointments.addAll(appointmentService.findByPatient(currentPatient.getPatientId()));
        detailAppointmentTableController.setItems(appointments);
        refreshMedicalRecords();
        refreshPrescriptions();
        refreshLabOrders();
    } catch (Exception e) {
        toastError("Failed to load appointments: " + e.getMessage());
    }
}

private void refreshMedicalRecords() {
    try {
        medicalRecords.clear();
        for (AppointmentDTO appointment : appointments) {
            try {
                medicalRecords.add(medicalRecordService.findByAppointment(appointment.getAppointmentId()));
            } catch (ResourceNotFoundException ignored) {
            }
        }
        detailMedicalRecordTableController.setItems(medicalRecords);
    } catch (Exception e) {
        toastError("Failed to load medical records: " + e.getMessage());
    }
}

private void refreshPrescriptions() {
    try {
        prescriptions.clear();
        for (AppointmentDTO appointment : appointments) {
            try {
                prescriptions.add(prescriptionService.findByAppointment(appointment.getAppointmentId()));
            } catch (ResourceNotFoundException ignored) {
            }
        }
        detailPrescriptionTableController.setItems(prescriptions);
    } catch (Exception e) {
        toastError("Failed to load prescriptions: " + e.getMessage());
    }
}

private void refreshLabOrders() {
    try {
        labOrders.clear();
        for (AppointmentDTO appointment : appointments) {
            labOrders.addAll(labService.findOrdersByAppointment(appointment.getAppointmentId()));
        }
        detailLabOrderTableController.setItems(labOrders);
    } catch (Exception e) {
        toastError("Failed to load lab orders: " + e.getMessage());
    }
}

private void refreshInvoices() {
    if (currentPatient == null) return;
    try {
        invoices.clear();
        invoices.addAll(invoiceService.findByPatient(currentPatient.getPatientId()));
        detailInvoiceTableController.setItems(invoices);
    } catch (Exception e) {
        toastError("Failed to load invoices: " + e.getMessage());
    }
}

private void refreshAllTables() {
    vitalSignTableController.setItems(vitals);
    detailMedicalRecordTableController.setItems(medicalRecords);
    detailAppointmentTableController.setItems(appointments);
+ Thought: 481ms
Now the medical record dialog:
← Edit src\main\java\hospital\management\pages\patient\PatientDetailController.java

// ── Medical records ──────────────────────────────────────────────────

/** Opens the shared form dialog in Add mode (record == null) or Update mode. */
private void openRecordDialog(MedicalRecord record) {
private void openRecordDialog(MedicalRecordDTO record) {
    boolean addMode = record == null;

    LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
    EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
            formDialogController.setLoading(false);
            return;
        }

        MedicalRecord target = addMode ? new MedicalRecord() : record;
        if (addMode) {
            target.setRecordId(UUID.randomUUID().toString());
            target.setCreatedAt(LocalDateTime.now());
        } else {
            target.setUpdatedAt(LocalDateTime.now());
        try {
            CreateMedicalRecordDTO dto = new CreateMedicalRecordDTO(appt, diag, symptoms.getText(), notes.getText());
            if (addMode) {
                medicalRecordService.create(dto);
            } else {
                medicalRecordService.update(record.getRecordId(), dto);
            }
            refreshMedicalRecords();
            formDialogController.close();
            toastSuccess(addMode ? "Medical record added." : "Medical record updated.");
        } catch (AppException ex) {
            formDialogController.setError(ex.getMessage());
            formDialogController.setLoading(false);
        } catch (Exception ex) {
            formDialogController.setError("Failed to save medical record: " + ex.getMessage());
            formDialogController.setLoading(false);
        }
        target.setAppointmentId(appt);
        target.setDiagnosis(diag);
        target.setSymptoms(symptoms.getText());
        target.setNotes(notes.getText());

        if (addMode) medicalRecords.add(target);
        detailMedicalRecordTableController.setItems(medicalRecords);
        formDialogController.close();
        toastSuccess(addMode ? "Medical record added." : "Medical record updated.");
    });

    formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
    formDialogController.addField("Diagnosis", "fas-stethoscope", diagnosis);
Now the prescription dialog in PatientDetailController:
← Edit src\main\java\hospital\management\pages\patient\PatientDetailController.java
}

// ── Prescriptions ─────────────────────────────────────────────────────

/** Opens the shared form dialog in Add mode (prescription == null) or Update mode. */
private void openPrescriptionDialog(Prescription prescription) {
    boolean addMode = prescription == null;

/** Opens the shared form dialog to issue a new prescription for the current patient. */
private void openPrescriptionDialog() {
    LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
    EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
    DatePicker dateIssued   = new DatePicker();

    LoadingIdComboBox medicationField = new LoadingIdComboBox();
    EntityIdComboBox medicationId = medicationField.getComboBox();
    TextField dosage        = new TextField();
    TextField quantity      = new TextField();
    TextField instructions  = new TextField();
    dosage.setPromptText("Dosage");
    quantity.setPromptText("Qty");
    instructions.setPromptText("Instructions");
    Button addItemBtn    = new Button("Add Item");
    Button removeItemBtn = new Button("Remove Selected");
    ListView<String> itemsList = new ListView<>();
    itemsList.setPrefHeight(110);

    appointmentId.getStyleClass().add("form-combo");
    dateIssued.getStyleClass().add("form-date-picker");
    List.of(dosage, quantity, instructions).forEach(f -> f.getStyleClass().add("form-input"));
    medicationId.getStyleClass().add("form-combo");
    addItemBtn.getStyleClass().add("secondary-button");
    removeItemBtn.getStyleClass().add("secondary-button");

    List<Control> prescriptionOtherFields = List.of(dateIssued);
    prescriptionOtherFields.forEach(f -> f.setDisable(true));
    List<Control> itemEditorFields = List.of(dosage, quantity, instructions, addItemBtn, removeItemBtn);
    itemEditorFields.forEach(f -> f.setDisable(true));

    if (!addMode) {
        dateIssued.setValue(prescription.getDateIssued());
    }
    List<CreatePrescriptionItemDTO> draftItems = new ArrayList<>();

    formDialogController.open(addMode ? "New Prescription" : "Update Prescription", "fas-prescription", addMode, v -> {
    addItemBtn.setOnAction(e -> {
        String medId = medicationId.getSelectedId();
        String medLabel = medicationId.getValue() == null ? "" : medicationId.getValue().label();
        String qtyText = quantity.getText() == null ? "" : quantity.getText().trim();
        if (medId == null) {
            toastError("Select a medication first.");
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(qtyText);
        } catch (NumberFormatException ex) {
            toastError("Quantity must be a whole number.");
            return;
        }
        if (qty <= 0) {
            toastError("Quantity must be greater than zero.");
            return;
        }
        draftItems.add(new CreatePrescriptionItemDTO(medId, dosage.getText(), qty, instructions.getText()));
        itemsList.getItems().add(medLabel + " | " + dosage.getText() + " | qty " + qty
                + (instructions.getText() == null || instructions.getText().isBlank() ? "" : " | " + instructions.getText()));
        dosage.clear();
        quantity.clear();
        instructions.clear();
    });

    removeItemBtn.setOnAction(e -> {
        int index = itemsList.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            toastError("Select an item in the list to remove it.");
            return;
        }
        itemsList.getItems().remove(index);
        draftItems.remove(index);
    });

    VBox itemsBox = new VBox(6);
    HBox itemInputRow = new HBox(6, medicationField, dosage, quantity, instructions, addItemBtn);
    itemInputRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    itemsBox.getChildren().addAll(itemInputRow, itemsList, removeItemBtn);

    formDialogController.open("New Prescription", "fas-prescription", true, v -> {
        String apptId = appointmentId.getSelectedId();
        if (apptId == null || dateIssued.getValue() == null) {
            formDialogController.setError("Appointment and date issued are required.");
            formDialogController.setLoading(false);
            return;
        }
        if (draftItems.isEmpty()) {
            formDialogController.setError("A prescription must include at least one medication item.");
            formDialogController.setLoading(false);
            return;
        }

        Prescription target = addMode ? new Prescription() : prescription;
        if (addMode) target.setPrescriptionId(UUID.randomUUID().toString());
        target.setAppointmentId(apptId);
        target.setDateIssued(dateIssued.getValue());

        if (addMode) prescriptions.add(target);
        detailPrescriptionTableController.setItems(prescriptions);
        formDialogController.close();
        toastSuccess(addMode ? "Prescription added." : "Prescription updated.");
        try {
            prescriptionService.issue(new CreatePrescriptionDTO(apptId, dateIssued.getValue(), List.copyOf(draftItems)));
            refreshPrescriptions();
            formDialogController.close();
            toastSuccess("Prescription issued.");
        } catch (AppException ex) {
            formDialogController.setError(ex.getMessage());
            formDialogController.setLoading(false);
        } catch (Exception ex) {
            formDialogController.setError("Failed to issue prescription: " + ex.getMessage());
            formDialogController.setLoading(false);
        }
    });

    formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
    formDialogController.addField("Date Issued", "fas-calendar", dateIssued);
    formDialogController.addField("Medication", "fas-pills", medicationField);
    formDialogController.addRow(itemsBox);

    loadAppointmentDropdown(appointmentIdField, prescriptionOtherFields, addMode ? null : prescription.getAppointmentId());
    loadPrescriptionDropdowns(appointmentIdField, medicationField, prescriptionOtherFields, itemEditorFields);
}

/** Loads the appointment and medication dropdown options asynchronously for the prescription dialog. */
private void loadPrescriptionDropdowns(LoadingIdComboBox appointmentIdField, LoadingIdComboBox medicationField,
                                       List<Control> otherFields, List<Control> itemEditorFields) {
    EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
    EntityIdComboBox medicationId = medicationField.getComboBox();

    appointmentIdField.setLoading(true);
    medicationField.setLoading(true);
    formDialogController.setLoading(true);

    java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger(2);
    Runnable onOneLoaded = () -> {
        if (pending.decrementAndGet() == 0) {
            otherFields.forEach(f -> f.setDisable(false));
            itemEditorFields.forEach(f -> f.setDisable(false));
            formDialogController.setLoading(false);
        }
    };

    AsyncJobRunner.submit(
        () -> appointmentService.findAll(CursorPagination.firstPage(1000)).getItems(),
        items -> {
            appointmentId.setOptions(items.stream()
                    .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                            a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                    .toList());
            if (currentPatient != null) {
                appointmentId.setOptions(items.stream()
                        .filter(a -> a.getAppointmentId() != null)
                        .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                                a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                        .toList());
            }
            appointmentIdField.setLoading(false);
            onOneLoaded.run();
        },
        ex -> {
            appointmentIdField.setLoading(false);
            toastError("Failed to load appointments: " + ex.getMessage());
            onOneLoaded.run();
        });

    AsyncJobRunner.submit(
        pharmacyService::findAllMedications,
        items -> {
            medicationId.setOptions(items.stream()
                    .map(m -> new EntityIdComboBox.Option(m.getMedicationId(), m.getName())).toList());
            medicationField.setLoading(false);
            onOneLoaded.run();
        },
        ex -> {
            medicationField.setLoading(false);
            toastError("Failed to load medications: " + ex.getMessage());
            onOneLoaded.run();
        });
}

/** Loads the appointment dropdown options asynchronously (shared by the Medical Record and
 *  Prescription dialogs), showing its own spinner while data is in flight and keeping the
 *  rest of the calling form disabled until it finishes loading. */
private void loadAppointmentDropdown(LoadingIdComboBox appointmentIdField, List<Control> otherFields, String existingAppointmentId) {
+ Thought: 1.1s
I left a redundant duplicate in loadPrescriptionDropdowns — let me fix it:
← Edit src\main\java\hospital\management\pages\patient\PatientDetailController.java
    appointmentId.setOptions(items.stream()
            .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                    a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
            .toList());
    if (currentPatient != null) {
        appointmentId.setOptions(items.stream()
                .filter(a -> a.getAppointmentId() != null)
                .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                        a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                .toList());
    }
    appointmentIdField.setLoading(false);
    onOneLoaded.run();
},
ex -> {