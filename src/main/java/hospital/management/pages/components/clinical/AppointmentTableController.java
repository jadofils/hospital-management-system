package hospital.management.pages.components.clinical;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.model.enums.AppointmentStatus;
import hospital.management.backend.model.enums.PaymentStatus;
import hospital.management.backend.service.lookup.EntityLookupService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class AppointmentTableController extends PaginatedTableController<AppointmentDTO> {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EntityLookupService lookupService = new EntityLookupService();

    @FXML private TableColumn<AppointmentDTO, String> patientIdColumn;
    @FXML private TableColumn<AppointmentDTO, String> doctorIdColumn;
    @FXML private TableColumn<AppointmentDTO, String> dateColumn;
    @FXML private TableColumn<AppointmentDTO, String> statusColumn;
    @FXML private TableColumn<AppointmentDTO, Void>   changeStatusColumn;
    @FXML private TableColumn<AppointmentDTO, Void>   billingColumn;
    @FXML private TableColumn<AppointmentDTO, String> reasonColumn;
    @FXML private TableColumn<AppointmentDTO, Void>   actionsColumn;

    private Consumer<AppointmentDTO> onChangeStatus;

    /** Registers the row-level "change status" callback used by the changeStatusColumn button. */
    public void setOnChangeStatus(Consumer<AppointmentDTO> onChangeStatus) {
        this.onChangeStatus = onChangeStatus;
    }

    @Override
    protected void configureColumns() {
        patientIdColumn.setText("Patient");
        patientIdColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(resolveLabel(() -> lookupService.patientLabel(cell.getValue().getPatientId()))));
        doctorIdColumn.setText("Doctor");
        doctorIdColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(resolveLabel(() -> lookupService.doctorLabel(cell.getValue().getDoctorId()))));
        dateColumn.setCellValueFactory(cell -> {
            var date = cell.getValue().getAppointmentDate();
            return new SimpleStringProperty(date != null ? date.format(DISPLAY_FMT) : "");
        });
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        wireSingleActionColumn(changeStatusColumn, "fas-flag", "Change appointment status",
                item -> { if (onChangeStatus != null) onChangeStatus.accept(item); });
        wireBillingColumn();
        reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        addSortOption("Patient", patientIdColumn);
        addSortOption("Doctor", doctorIdColumn);
        addSortOption("Date", dateColumn);
        addSortOption("Status", statusColumn);
        addSortOption("Reason", reasonColumn);
        wireActionsColumn(actionsColumn);
    }

    /**
     * Billing column: a "Paid"/"Partially Paid"/"Unpaid" text badge on completed
     * appointments — the same "needs billing" definition the page's filter dropdown
     * uses treats anything short of fully paid as needing billing. Paid rows always
     * render their badge regardless of appointment status; never-completed,
     * not-yet-billed rows render blank.
     */
    private void wireBillingColumn() {
        billingColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().size() <= getIndex()) {
                    setGraphic(null);
                    return;
                }
                AppointmentDTO appointment = getTableView().getItems().get(getIndex());
                boolean completed = AppointmentStatus.COMPLETED.getDbValue().equalsIgnoreCase(appointment.getStatus());
                PaymentStatus billing = parseBillingStatus(appointment.getBillingStatus());
                if (billing == PaymentStatus.PAID) {
                    setGraphic(badge("Paid", "status-paid", "Invoice paid"));
                } else if (completed && billing == PaymentStatus.PARTIALLY_PAID) {
                    setGraphic(badge("Partially Paid", "status-partially-paid", "Invoice partially paid"));
                } else if (completed) {
                    setGraphic(badge("Unpaid", "status-cancelled", "No paid invoice for this completed appointment"));
                } else {
                    setGraphic(null);
                }
            }
        });
    }

    private static PaymentStatus parseBillingStatus(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) return PaymentStatus.UNPAID;
        try {
            return PaymentStatus.fromDbValue(dbValue);
        } catch (IllegalArgumentException e) {
            return PaymentStatus.UNPAID;
        }
    }

    private static Label badge(String text, String styleClass, String tooltip) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("status-badge", styleClass);
        Tooltip.install(badge, new Tooltip(tooltip));
        return badge;
    }

    @Override
    protected boolean matches(AppointmentDTO appointment, String lowerQuery) {
        String dateStr = appointment.getAppointmentDate() != null
            ? appointment.getAppointmentDate().format(DISPLAY_FMT) : "";
        return safe(appointment.getPatientId()).contains(lowerQuery)
            || safe(appointment.getDoctorId()).contains(lowerQuery)
            || safe(appointment.getStatus()).contains(lowerQuery)
            || safe(appointment.getBillingStatus()).contains(lowerQuery)
            || safe(appointment.getReason()).contains(lowerQuery)
            || dateStr.contains(lowerQuery);
    }

    private static String safe(String s) { return s == null ? "" : s.toLowerCase(); }
}
