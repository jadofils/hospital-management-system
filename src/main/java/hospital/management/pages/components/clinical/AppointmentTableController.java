package hospital.management.pages.components.clinical;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.model.enums.AppointmentStatus;
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
        wireSingleActionColumn(changeStatusColumn, "fas-flag",
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
     * Billing column: a plain "Paid"/"Unpaid" text badge on completed appointments
     * whose invoice is missing or unpaid — the same "needs billing" definition the
     * page's filter dropdown uses. Paid (or never-completed) rows render blank.
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
                boolean paid = "paid".equalsIgnoreCase(appointment.getBillingStatus());
                if (completed && !paid) {
                    Label badge = new Label("Unpaid");
                    badge.getStyleClass().add("status-badge");
                    badge.getStyleClass().add("status-cancelled");
                    Tooltip.install(badge, new Tooltip("No paid invoice for this completed appointment"));
                    setGraphic(badge);
                } else if (paid) {
                    Label badge = new Label("Paid");
                    badge.getStyleClass().add("status-badge");
                    Tooltip.install(badge, new Tooltip("Invoice paid"));
                    setGraphic(badge);
                } else {
                    setGraphic(null);
                }
            }
        });
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
