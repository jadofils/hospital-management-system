package hospital.management.backend.service.notification;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.PermissionDAOImpl;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.dao.auth.RolePermissionDAOImpl;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dao.auth.interfaces.UserDAO;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.clinical.MedicalRecordDAOImpl;
import hospital.management.backend.dao.clinical.interfaces.AppointmentDAO;
import hospital.management.backend.dao.clinical.interfaces.MedicalRecordDAO;
import hospital.management.backend.dao.department.ReferralDAOImpl;
import hospital.management.backend.dao.department.interfaces.ReferralDAO;
import hospital.management.backend.dao.finance.InvoiceDAOImpl;
import hospital.management.backend.dao.finance.interfaces.InvoiceDAO;
import hospital.management.backend.dao.lab.LabOrderDAOImpl;
import hospital.management.backend.dao.lab.LabResultDAOImpl;
import hospital.management.backend.dao.lab.interfaces.LabOrderDAO;
import hospital.management.backend.dao.lab.interfaces.LabResultDAO;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.patient.PatientFeedbackDAOImpl;
import hospital.management.backend.dao.patient.VitalSignDAOImpl;
import hospital.management.backend.dao.patient.interfaces.PatientDAO;
import hospital.management.backend.dao.patient.interfaces.PatientFeedbackDAO;
import hospital.management.backend.dao.patient.interfaces.VitalSignDAO;
import hospital.management.backend.dao.pharmacy.PrescriptionDAOImpl;
import hospital.management.backend.dao.pharmacy.interfaces.PrescriptionDAO;
import hospital.management.backend.dto.notification.NotificationDTO;
import hospital.management.backend.dto.notification.NotificationTopics;
import hospital.management.backend.model.doctor.Referral;
import hospital.management.backend.model.enums.RoleName;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.model.lab.LabOrder;
import hospital.management.backend.model.lab.LabResult;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.MedicalRecord;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.model.patient.PatientFeedback;
import hospital.management.backend.model.patient.VitalSign;
import hospital.management.backend.model.pharmacy.Prescription;
import hospital.management.backend.model.user.User;
import hospital.management.backend.service.backup.BackupManifest;
import hospital.management.backend.service.auth.RoleServiceImpl;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.RoleService;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.backend.utils.pipes.AsyncJobRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Subscribes to the business events that already fire via {@link EventBus}
 * (appointment booked/updated/cancelled, prescription created, lab result
 * ready) and turns each into a real {@link NotificationServiceImpl#createNotification}
 * call, so the affected user actually gets notified instead of the event
 * being fired into the void.
 *
 * <p>This app has no patient portal/login — {@code RoleName} is
 * Admin/Doctor/Receptionist/Analyst/Pharmacist only, patients never log in —
 * so "the affected user" for every one of these events is the doctor on the
 * appointment/prescription/lab order, resolved via {@link UserDAO#findByDoctorId}.
 * Patient context is still included in the notification payload for display
 * text. A doctor with no login account has nothing to notify — the handler
 * silently skips.
 *
 * <p>{@link EventBus} always invokes listeners on the JavaFX Application
 * Thread, so every handler here offloads its actual work (blocking JDBC +
 * {@code createNotification}, which itself inserts + emails + mirrors to
 * Mongo) to {@link AsyncJobRunner} rather than running it inline.
 *
 * <p>Events fall into three groups:
 * <ul>
 *   <li><b>Single-recipient</b> — resolves one doctor via {@link UserDAO#findByDoctorId}
 *       (appointments, prescriptions, lab orders/results, medical records, invoices,
 *       vital signs, feedback, referrals — the last notifies both doctors involved —
 *       and doctor/doctor-schedule changes, which already carry the doctor id as the
 *       event payload).</li>
 *   <li><b>Role-broadcast</b> — entities with no FK to any doctor/user at all
 *       (patients, allergies, medications, inventory) or pure admin/RBAC actions
 *       (users, roles, permissions, departments) notify everyone holding a given
 *       role via {@link #resolveRoleUserIds}, excluding the actor.</li>
 *   <li><b>Not wired</b> — {@code SESSION_EXPIRED}/{@code LAB_RESULT_UPDATED}/
 *       {@code MEDICATION_UPDATED} never fire anywhere (dead events); session
 *       lifecycle ({@code USER_LOGGED_IN/OUT}), pure logging ({@code AUDIT_LOG_RECORDED},
 *       {@code SYSTEM_LOG_RECORDED}), and scheduled ops events ({@code DATA_CLEANING_*},
 *       {@code BACKUP_*}, which already have their own live feedback in the
 *       Retention/Backup settings tabs) are deliberately not turned into notifications.</li>
 * </ul>
 */
public final class NotificationEventListener {

    private static final AppLogger logger = AppLogger.getLogger(NotificationEventListener.class);

    private static final UserDAO userDAO = new UserDAOImpl();
    private static final AppointmentDAO appointmentDAO = new AppointmentDAOImpl();
    private static final PrescriptionDAO prescriptionDAO = new PrescriptionDAOImpl();
    private static final LabOrderDAO labOrderDAO = new LabOrderDAOImpl();
    private static final LabResultDAO labResultDAO = new LabResultDAOImpl();
    private static final PatientDAO patientDAO = new PatientDAOImpl();
    private static final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAOImpl();
    private static final InvoiceDAO invoiceDAO = new InvoiceDAOImpl();
    private static final VitalSignDAO vitalSignDAO = new VitalSignDAOImpl();
    private static final PatientFeedbackDAO patientFeedbackDAO = new PatientFeedbackDAOImpl();
    private static final ReferralDAO referralDAO = new ReferralDAOImpl();
    private static final RoleService roleService = new RoleServiceImpl(
        new RoleDAOImpl(), new UserRoleDAOImpl(), new RolePermissionDAOImpl(), new PermissionDAOImpl());
    private static final NotificationServiceImpl notificationService =
        new NotificationServiceImpl(new UserServiceImpl(userDAO));

    private static boolean started = false;

    private NotificationEventListener() {}

    /** Call once from Main.start(). Subsequent calls are no-ops. */
    public static synchronized void start() {
        if (started) return;
        started = true;

        EventBus.subscribe(AppEventType.APPOINTMENT_BOOKED,
            e -> onAppointmentEvent(asId(e.getPayload()), NotificationTopics.APPOINTMENT_CREATED));
        EventBus.subscribe(AppEventType.APPOINTMENT_UPDATED,
            e -> onAppointmentEvent(asId(e.getPayload()), NotificationTopics.APPOINTMENT_UPDATED));
        EventBus.subscribe(AppEventType.APPOINTMENT_CANCELLED,
            e -> onAppointmentEvent(asId(e.getPayload()), NotificationTopics.APPOINTMENT_CANCELLED));
        EventBus.subscribe(AppEventType.PRESCRIPTION_CREATED,
            e -> onPrescriptionCreated(asId(e.getPayload())));
        EventBus.subscribe(AppEventType.LAB_RESULT_READY,
            e -> onLabResultReady(asId(e.getPayload())));

        // ── Single-recipient (doctor) — clinical/billing/lab domains ────────
        EventBus.subscribe(AppEventType.MEDICAL_RECORD_CREATED,
            e -> onMedicalRecordEvent(asId(e.getPayload()), NotificationTopics.MEDICAL_RECORD_CREATED));
        EventBus.subscribe(AppEventType.MEDICAL_RECORD_UPDATED,
            e -> onMedicalRecordEvent(asId(e.getPayload()), NotificationTopics.MEDICAL_RECORD_UPDATED));
        EventBus.subscribe(AppEventType.INVOICE_CREATED,
            e -> onInvoiceEvent(asId(e.getPayload()), NotificationTopics.INVOICE_CREATED));
        EventBus.subscribe(AppEventType.INVOICE_UPDATED,
            e -> onInvoiceEvent(asId(e.getPayload()), NotificationTopics.INVOICE_UPDATED));
        EventBus.subscribe(AppEventType.INVOICE_PAID,
            e -> onInvoiceEvent(asId(e.getPayload()), NotificationTopics.INVOICE_PAID));
        EventBus.subscribe(AppEventType.LAB_ORDER_CREATED,
            e -> onLabOrderEvent(asId(e.getPayload()), NotificationTopics.LAB_ORDER_CREATED));
        EventBus.subscribe(AppEventType.LAB_ORDER_UPDATED,
            e -> onLabOrderEvent(asId(e.getPayload()), NotificationTopics.LAB_ORDER_UPDATED));
        EventBus.subscribe(AppEventType.VITAL_SIGN_RECORDED,
            e -> onVitalSignRecorded(asId(e.getPayload())));
        EventBus.subscribe(AppEventType.PATIENT_FEEDBACK_SUBMITTED,
            e -> onPatientFeedbackSubmitted(asId(e.getPayload())));
        EventBus.subscribe(AppEventType.REFERRAL_CREATED,
            e -> onReferralEvent(asId(e.getPayload()), NotificationTopics.REFERRAL_CREATED));
        EventBus.subscribe(AppEventType.REFERRAL_UPDATED,
            e -> onReferralEvent(asId(e.getPayload()), NotificationTopics.REFERRAL_UPDATED));
        EventBus.subscribe(AppEventType.DOCTOR_UPDATED,
            e -> onDoctorOwnEvent(asId(e.getPayload()), NotificationTopics.DOCTOR_UPDATED));
        EventBus.subscribe(AppEventType.DOCTOR_DELETED,
            e -> onDoctorOwnEvent(asId(e.getPayload()), NotificationTopics.DOCTOR_DELETED));
        EventBus.subscribe(AppEventType.DOCTOR_SCHEDULE_UPDATED,
            e -> onDoctorOwnEvent(asId(e.getPayload()), NotificationTopics.DOCTOR_SCHEDULE_UPDATED));

        // ── Role-broadcast — no single natural recipient exists ─────────────
        EventBus.subscribe(AppEventType.PATIENT_CREATED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.PATIENT_CREATED, RoleName.RECEPTIONIST, "patientId"));
        EventBus.subscribe(AppEventType.PATIENT_UPDATED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.PATIENT_UPDATED, RoleName.RECEPTIONIST, "patientId"));
        EventBus.subscribe(AppEventType.PATIENT_DELETED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.PATIENT_DELETED, RoleName.RECEPTIONIST, "patientId"));
        EventBus.subscribe(AppEventType.PATIENT_ALLERGY_ADDED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.PATIENT_ALLERGY_ADDED, RoleName.RECEPTIONIST, "allergyId"));
        EventBus.subscribe(AppEventType.PATIENT_ALLERGY_REMOVED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.PATIENT_ALLERGY_REMOVED, RoleName.RECEPTIONIST, "allergyId"));
        EventBus.subscribe(AppEventType.MEDICATION_CREATED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.MEDICATION_CREATED, RoleName.PHARMACIST, "medicationId"));
        EventBus.subscribe(AppEventType.INVENTORY_UPDATED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.INVENTORY_UPDATED, RoleName.PHARMACIST, "inventoryId"));
        EventBus.subscribe(AppEventType.INVENTORY_LOW_STOCK,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.INVENTORY_LOW_STOCK, RoleName.PHARMACIST, "inventoryId"));
        EventBus.subscribe(AppEventType.USER_CREATED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.USER_CREATED, RoleName.ADMIN, "userId"));
        EventBus.subscribe(AppEventType.USER_UPDATED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.USER_UPDATED, RoleName.ADMIN, "userId"));
        EventBus.subscribe(AppEventType.USER_DELETED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.USER_DELETED, RoleName.ADMIN, "userId"));
        EventBus.subscribe(AppEventType.ROLE_CREATED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.ROLE_CREATED, RoleName.ADMIN, "roleId"));
        EventBus.subscribe(AppEventType.ROLE_UPDATED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.ROLE_UPDATED, RoleName.ADMIN, "roleId"));
        EventBus.subscribe(AppEventType.ROLE_DELETED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.ROLE_DELETED, RoleName.ADMIN, "roleId"));
        EventBus.subscribe(AppEventType.PERMISSION_CREATED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.PERMISSION_CREATED, RoleName.ADMIN, "permissionId"));
        EventBus.subscribe(AppEventType.PERMISSION_DELETED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.PERMISSION_DELETED, RoleName.ADMIN, "permissionId"));
        EventBus.subscribe(AppEventType.DEPARTMENT_CREATED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.DEPARTMENT_CREATED, RoleName.ADMIN, "departmentId"));
        EventBus.subscribe(AppEventType.DEPARTMENT_UPDATED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.DEPARTMENT_UPDATED, RoleName.ADMIN, "departmentId"));
        EventBus.subscribe(AppEventType.DEPARTMENT_DELETED,
            e -> onRoleBroadcast(asId(e.getPayload()), NotificationTopics.DEPARTMENT_DELETED, RoleName.ADMIN, "departmentId"));

        // ── Developer Dashboard (admin-audit) — self-notifying: the acting admin
        // wants their own confirmation of what they just did, so these do NOT
        // exclude the actor the way the role-broadcast topics above do.
        EventBus.subscribe(AppEventType.DB_OBJECT_CHANGED,
            e -> onAdminAuditEvent(String.valueOf(e.getPayload()), NotificationTopics.DB_OBJECT_CHANGED));
        EventBus.subscribe(AppEventType.MAINTENANCE_ACCESS_CHANGED,
            e -> onAdminAuditEvent(String.valueOf(e.getPayload()), NotificationTopics.MAINTENANCE_ACCESS_CHANGED));
        EventBus.subscribe(AppEventType.BACKUP_COMPLETED,
            e -> onBackupEvent(e.getPayload(), NotificationTopics.BACKUP_COMPLETED));
        EventBus.subscribe(AppEventType.BACKUP_FAILED,
            e -> onAdminAuditEvent(String.valueOf(e.getPayload()), NotificationTopics.BACKUP_FAILED));

        logger.info("NotificationEventListener started — every business event with a resolvable recipient is now wired.");
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private static void onAppointmentEvent(String appointmentId, String type) {
        if (appointmentId == null) return;
        String actorUserId = safeCurrentUserId();

        AsyncJobRunner.submit(() -> {
            Optional<Appointment> apptOpt = appointmentDAO.findById(appointmentId);
            if (apptOpt.isEmpty()) return null;
            Appointment appt = apptOpt.get();

            List<String> recipients = resolveDoctorUserIds(appt.getDoctorId(), actorUserId);
            if (recipients.isEmpty()) return null;

            NotificationDTO dto = new NotificationDTO();
            dto.setType(type);
            dto.setActorUserId(actorUserId);
            dto.setRecipients(recipients);
            dto.setChannels(List.of("in_app", "email"));
            dto.setPayload(java.util.Map.of(
                "appointmentId", appointmentId,
                "patientName", resolvePatientName(appt.getPatientId()),
                "appointmentDate", String.valueOf(appt.getAppointmentDate()),
                "reason", appt.getReason() == null ? "" : appt.getReason()
            ));
            notificationService.createNotification(dto);
            return null;
        }, ignored -> {}, ex -> logger.warn("Appointment notification failed: " + ex.getMessage()));
    }

    private static void onPrescriptionCreated(String prescriptionId) {
        if (prescriptionId == null) return;
        String actorUserId = safeCurrentUserId();

        AsyncJobRunner.submit(() -> {
            Optional<Prescription> rxOpt = prescriptionDAO.findById(prescriptionId);
            if (rxOpt.isEmpty()) return null;
            Prescription rx = rxOpt.get();

            Optional<Appointment> apptOpt = appointmentDAO.findById(rx.getAppointmentId());
            if (apptOpt.isEmpty()) return null;
            Appointment appt = apptOpt.get();

            List<String> recipients = resolveDoctorUserIds(appt.getDoctorId(), actorUserId);
            if (recipients.isEmpty()) return null;

            NotificationDTO dto = new NotificationDTO();
            dto.setType(NotificationTopics.PRESCRIPTION_CREATED);
            dto.setActorUserId(actorUserId);
            dto.setRecipients(recipients);
            dto.setChannels(List.of("in_app", "email"));
            dto.setPayload(java.util.Map.of(
                "prescriptionId", prescriptionId,
                "patientName", resolvePatientName(appt.getPatientId()),
                "dateIssued", String.valueOf(rx.getDateIssued())
            ));
            notificationService.createNotification(dto);
            return null;
        }, ignored -> {}, ex -> logger.warn("Prescription notification failed: " + ex.getMessage()));
    }

    private static void onLabResultReady(String labResultId) {
        if (labResultId == null) return;
        String actorUserId = safeCurrentUserId();

        AsyncJobRunner.submit(() -> {
            Optional<LabResult> resultOpt = labResultDAO.findById(labResultId);
            if (resultOpt.isEmpty()) return null;
            LabResult result = resultOpt.get();

            Optional<LabOrder> orderOpt = labOrderDAO.findById(result.getLabOrderId());
            if (orderOpt.isEmpty()) return null;
            LabOrder order = orderOpt.get();

            List<String> recipients = resolveDoctorUserIds(order.getDoctorId(), actorUserId);
            if (recipients.isEmpty()) return null;

            String patientName = "";
            Optional<Appointment> apptOpt = appointmentDAO.findById(order.getAppointmentId());
            if (apptOpt.isPresent()) patientName = resolvePatientName(apptOpt.get().getPatientId());

            NotificationDTO dto = new NotificationDTO();
            dto.setType(NotificationTopics.LAB_RESULT_READY);
            dto.setActorUserId(actorUserId);
            dto.setRecipients(recipients);
            dto.setChannels(List.of("in_app", "email"));
            dto.setPayload(java.util.Map.of(
                "labOrderId", order.getLabOrderId(),
                "testName", order.getTestName() == null ? "" : order.getTestName(),
                "patientName", patientName,
                "isAbnormal", Boolean.TRUE.equals(result.isIsAbnormal())
            ));
            notificationService.createNotification(dto);
            return null;
        }, ignored -> {}, ex -> logger.warn("Lab result notification failed: " + ex.getMessage()));
    }

    private static void onMedicalRecordEvent(String recordId, String type) {
        if (recordId == null) return;
        String actorUserId = safeCurrentUserId();

        AsyncJobRunner.submit(() -> {
            Optional<MedicalRecord> recordOpt = medicalRecordDAO.findById(recordId);
            if (recordOpt.isEmpty()) return null;
            MedicalRecord record = recordOpt.get();

            Optional<Appointment> apptOpt = appointmentDAO.findById(record.getAppointmentId());
            if (apptOpt.isEmpty()) return null;
            Appointment appt = apptOpt.get();

            List<String> recipients = resolveDoctorUserIds(appt.getDoctorId(), actorUserId);
            if (recipients.isEmpty()) return null;

            NotificationDTO dto = new NotificationDTO();
            dto.setType(type);
            dto.setActorUserId(actorUserId);
            dto.setRecipients(recipients);
            dto.setChannels(List.of("in_app", "email"));
            dto.setPayload(java.util.Map.of(
                "recordId", recordId,
                "patientName", resolvePatientName(appt.getPatientId()),
                "diagnosis", record.getDiagnosis() == null ? "" : record.getDiagnosis()
            ));
            notificationService.createNotification(dto);
            return null;
        }, ignored -> {}, ex -> logger.warn("Medical record notification failed: " + ex.getMessage()));
    }

    private static void onInvoiceEvent(String invoiceId, String type) {
        if (invoiceId == null) return;
        String actorUserId = safeCurrentUserId();

        AsyncJobRunner.submit(() -> {
            Optional<Invoice> invoiceOpt = invoiceDAO.findById(invoiceId);
            if (invoiceOpt.isEmpty()) return null;
            Invoice invoice = invoiceOpt.get();

            Optional<Appointment> apptOpt = appointmentDAO.findById(invoice.getAppointmentId());
            if (apptOpt.isEmpty()) return null;
            Appointment appt = apptOpt.get();

            List<String> recipients = resolveDoctorUserIds(appt.getDoctorId(), actorUserId);
            if (recipients.isEmpty()) return null;

            NotificationDTO dto = new NotificationDTO();
            dto.setType(type);
            dto.setActorUserId(actorUserId);
            dto.setRecipients(recipients);
            dto.setChannels(List.of("in_app", "email"));
            dto.setPayload(java.util.Map.of(
                "invoiceId", invoiceId,
                "patientName", resolvePatientName(invoice.getPatientId()),
                "totalAmount", String.valueOf(invoice.getTotalAmount()),
                "paymentStatus", invoice.getPaymentStatus() == null ? "" : invoice.getPaymentStatus()
            ));
            notificationService.createNotification(dto);
            return null;
        }, ignored -> {}, ex -> logger.warn("Invoice notification failed: " + ex.getMessage()));
    }

    private static void onLabOrderEvent(String labOrderId, String type) {
        if (labOrderId == null) return;
        String actorUserId = safeCurrentUserId();

        AsyncJobRunner.submit(() -> {
            Optional<LabOrder> orderOpt = labOrderDAO.findById(labOrderId);
            if (orderOpt.isEmpty()) return null;
            LabOrder order = orderOpt.get();

            List<String> recipients = resolveDoctorUserIds(order.getDoctorId(), actorUserId);
            if (recipients.isEmpty()) return null;

            String patientName = "";
            Optional<Appointment> apptOpt = appointmentDAO.findById(order.getAppointmentId());
            if (apptOpt.isPresent()) patientName = resolvePatientName(apptOpt.get().getPatientId());

            NotificationDTO dto = new NotificationDTO();
            dto.setType(type);
            dto.setActorUserId(actorUserId);
            dto.setRecipients(recipients);
            dto.setChannels(List.of("in_app", "email"));
            dto.setPayload(java.util.Map.of(
                "labOrderId", labOrderId,
                "testName", order.getTestName() == null ? "" : order.getTestName(),
                "patientName", patientName
            ));
            notificationService.createNotification(dto);
            return null;
        }, ignored -> {}, ex -> logger.warn("Lab order notification failed: " + ex.getMessage()));
    }

    private static void onVitalSignRecorded(String vitalId) {
        if (vitalId == null) return;
        String actorUserId = safeCurrentUserId();

        AsyncJobRunner.submit(() -> {
            Optional<VitalSign> vitalOpt = vitalSignDAO.findById(vitalId);
            if (vitalOpt.isEmpty()) return null;
            VitalSign vital = vitalOpt.get();

            Optional<Appointment> apptOpt = appointmentDAO.findById(vital.getAppointmentId());
            if (apptOpt.isEmpty()) return null;
            Appointment appt = apptOpt.get();

            List<String> recipients = resolveDoctorUserIds(appt.getDoctorId(), actorUserId);
            if (recipients.isEmpty()) return null;

            NotificationDTO dto = new NotificationDTO();
            dto.setType(NotificationTopics.VITAL_SIGN_RECORDED);
            dto.setActorUserId(actorUserId);
            dto.setRecipients(recipients);
            dto.setChannels(List.of("in_app", "email"));
            dto.setPayload(java.util.Map.of(
                "vitalId", vitalId,
                "patientName", resolvePatientName(appt.getPatientId())
            ));
            notificationService.createNotification(dto);
            return null;
        }, ignored -> {}, ex -> logger.warn("Vital sign notification failed: " + ex.getMessage()));
    }

    /**
     * Covers both {@code FeedbackServiceImpl} and {@code PatientFeedbackServiceImpl}
     * — two near-duplicate service classes that both publish this same event type,
     * so one subscription handles both. {@code appointmentId} is nullable on
     * feedback; when absent there's no reliable recipient, so the handler skips.
     */
    private static void onPatientFeedbackSubmitted(String feedbackId) {
        if (feedbackId == null) return;
        String actorUserId = safeCurrentUserId();

        AsyncJobRunner.submit(() -> {
            Optional<PatientFeedback> feedbackOpt = patientFeedbackDAO.findById(feedbackId);
            if (feedbackOpt.isEmpty()) return null;
            PatientFeedback feedback = feedbackOpt.get();
            if (feedback.getAppointmentId() == null) return null;

            Optional<Appointment> apptOpt = appointmentDAO.findById(feedback.getAppointmentId());
            if (apptOpt.isEmpty()) return null;
            Appointment appt = apptOpt.get();

            List<String> recipients = resolveDoctorUserIds(appt.getDoctorId(), actorUserId);
            if (recipients.isEmpty()) return null;

            NotificationDTO dto = new NotificationDTO();
            dto.setType(NotificationTopics.PATIENT_FEEDBACK_SUBMITTED);
            dto.setActorUserId(actorUserId);
            dto.setRecipients(recipients);
            dto.setChannels(List.of("in_app", "email"));
            dto.setPayload(java.util.Map.of(
                "feedbackId", feedbackId,
                "patientName", resolvePatientName(appt.getPatientId())
            ));
            notificationService.createNotification(dto);
            return null;
        }, ignored -> {}, ex -> logger.warn("Patient feedback notification failed: " + ex.getMessage()));
    }

    /**
     * A referral has two natural parties — the referring doctor and the
     * referred-to doctor — both notified (minus the actor, minus duplicates
     * if they happen to be the same doctor).
     */
    private static void onReferralEvent(String referralId, String type) {
        if (referralId == null) return;
        String actorUserId = safeCurrentUserId();

        AsyncJobRunner.submit(() -> {
            Optional<Referral> referralOpt = referralDAO.findById(referralId);
            if (referralOpt.isEmpty()) return null;
            Referral referral = referralOpt.get();

            List<String> recipients = new ArrayList<>(resolveDoctorUserIds(referral.getReferringDoctorId(), actorUserId));
            for (String id : resolveDoctorUserIds(referral.getReferredToDoctorId(), actorUserId)) {
                if (!recipients.contains(id)) recipients.add(id);
            }
            if (recipients.isEmpty()) return null;

            NotificationDTO dto = new NotificationDTO();
            dto.setType(type);
            dto.setActorUserId(actorUserId);
            dto.setRecipients(recipients);
            dto.setChannels(List.of("in_app", "email"));
            dto.setPayload(java.util.Map.of(
                "referralId", referralId,
                "reason", referral.getReason() == null ? "" : referral.getReason(),
                "status", referral.getStatus() == null ? "" : referral.getStatus()
            ));
            notificationService.createNotification(dto);
            return null;
        }, ignored -> {}, ex -> logger.warn("Referral notification failed: " + ex.getMessage()));
    }

    /**
     * Doctor/doctor-schedule events already publish the doctor's own id as the
     * event payload (confirmed at the publish call sites) — no DAO hop needed
     * to find "who this is about" before resolving their login account.
     */
    private static void onDoctorOwnEvent(String doctorId, String type) {
        if (doctorId == null) return;
        String actorUserId = safeCurrentUserId();

        AsyncJobRunner.submit(() -> {
            List<String> recipients = resolveDoctorUserIds(doctorId, actorUserId);
            if (recipients.isEmpty()) return null;

            NotificationDTO dto = new NotificationDTO();
            dto.setType(type);
            dto.setActorUserId(actorUserId);
            dto.setRecipients(recipients);
            dto.setChannels(List.of("in_app", "email"));
            dto.setPayload(java.util.Map.of("doctorId", doctorId));
            notificationService.createNotification(dto);
            return null;
        }, ignored -> {}, ex -> logger.warn("Doctor notification failed: " + ex.getMessage()));
    }

    /**
     * For entities with no FK to any doctor/user (patients, allergies,
     * medications, inventory) and pure admin/RBAC actions (users, roles,
     * permissions, departments) — notifies every user holding {@code role},
     * excluding the actor. {@code entityIdKey} names the raw id in the
     * payload for display purposes only (e.g. "patientId", "roleId").
     */
    private static void onRoleBroadcast(String entityId, String type, RoleName role, String entityIdKey) {
        if (entityId == null) return;
        String actorUserId = safeCurrentUserId();

        AsyncJobRunner.submit(() -> {
            List<String> recipients = resolveRoleUserIds(role, actorUserId);
            if (recipients.isEmpty()) return null;

            NotificationDTO dto = new NotificationDTO();
            dto.setType(type);
            dto.setActorUserId(actorUserId);
            dto.setRecipients(recipients);
            // In-app only, deliberately no email — these fire on ordinary, frequent CRUD
            // (e.g. every patient create/update) and would otherwise spam every
            // Receptionist/Pharmacist/Admin's inbox on each one.
            dto.setChannels(List.of("in_app"));
            dto.setPayload(java.util.Map.of(entityIdKey, entityId));
            notificationService.createNotification(dto);
            return null;
        }, ignored -> {}, ex -> logger.warn("Role-broadcast notification failed: " + ex.getMessage()));
    }

    /**
     * Developer Dashboard admin-audit events (index/view/routine drop or
     * regenerate, maintenance grant/revoke/settings save, backup failure).
     * Notifies every Admin, deliberately INCLUDING the actor — unlike the
     * role-broadcast handler above, this is a self-notifying confirmation
     * ("your requested action completed") rather than a "someone else changed
     * a shared resource" tell.
     */
    private static void onAdminAuditEvent(String description, String type) {
        if (description == null) return;

        AsyncJobRunner.submit(() -> {
            List<String> recipients = resolveAllAdminUserIds();
            if (recipients.isEmpty()) return null;

            NotificationDTO dto = new NotificationDTO();
            dto.setType(type);
            dto.setActorUserId(safeCurrentUserId());
            dto.setRecipients(recipients);
            dto.setChannels(List.of("in_app"));
            dto.setPayload(java.util.Map.of("description", description));
            notificationService.createNotification(dto);
            return null;
        }, ignored -> {}, ex -> logger.warn("Admin-audit notification failed: " + ex.getMessage()));
    }

    /**
     * {@code BACKUP_COMPLETED}'s payload is a {@link BackupManifest} (published
     * by both the scheduled {@code BackupDaemon} cycle and the Developer
     * Dashboard's manual "Backup Now" button) — described here rather than
     * relying on {@code toString()} so the notification reads like a sentence.
     */
    private static void onBackupEvent(Object payload, String type) {
        String description = payload instanceof BackupManifest manifest
            ? "Backup " + manifest.status + " (" + manifest.backupId + ") — " + manifest.postgresTables.size()
                + " table(s)" + (manifest.mongoSkipped ? ", MongoDB skipped" : "") + ". Saved to " + manifest.directoryPath
            : String.valueOf(payload);
        onAdminAuditEvent(description, type);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resolves the login account linked to a doctor, excluding the acting user
     * (a doctor updating their own appointment doesn't need to be told about
     * it). Returns an empty list if the doctor has no login account.
     */
    private static List<String> resolveDoctorUserIds(String doctorId, String actorUserId) {
        if (doctorId == null) return List.of();
        try {
            Optional<User> userOpt = userDAO.findByDoctorId(doctorId);
            if (userOpt.isEmpty()) return List.of();
            String userId = userOpt.get().getUserId();
            if (userId.equals(actorUserId)) return List.of();
            return List.of(userId);
        } catch (Exception e) {
            logger.warn("Could not resolve doctor's user account: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Resolves every user id currently holding {@code role}, excluding the
     * acting user (an Admin performing an admin action doesn't need to be
     * told about their own action). Empty list if the role has no members.
     */
    private static List<String> resolveRoleUserIds(RoleName role, String actorUserId) {
        try {
            List<String> userIds = roleService.findUserIdsForRole(role.getDbValue());
            List<String> recipients = new ArrayList<>();
            for (String userId : userIds) {
                if (!userId.equals(actorUserId)) recipients.add(userId);
            }
            return recipients;
        } catch (Exception e) {
            logger.warn("Could not resolve users for role " + role + ": " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Every Admin's user id, including the current actor — used only by the
     * self-notifying admin-audit handlers above. {@link #resolveRoleUserIds}
     * (which excludes the actor) is still what every role-broadcast business
     * event uses.
     */
    private static List<String> resolveAllAdminUserIds() {
        try {
            return roleService.findUserIdsForRole(RoleName.ADMIN.getDbValue());
        } catch (Exception e) {
            logger.warn("Could not resolve admin users: " + e.getMessage());
            return List.of();
        }
    }

    private static String resolvePatientName(String patientId) {
        if (patientId == null) return "";
        try {
            Optional<Patient> patientOpt = patientDAO.findById(patientId);
            if (patientOpt.isEmpty()) return "";
            Patient p = patientOpt.get();
            return (p.getFirstName() == null ? "" : p.getFirstName()) + " "
                + (p.getLastName() == null ? "" : p.getLastName());
        } catch (Exception e) {
            return "";
        }
    }

    private static String asId(Object payload) {
        return payload == null ? null : String.valueOf(payload);
    }

    private static String safeCurrentUserId() {
        try {
            return SessionManager.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
