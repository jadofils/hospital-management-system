package hospital.management.pages.components.doctor;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.doctor.Referral;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ReferralTableController extends PaginatedTableController<Referral> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<Referral, String> referralIdCol;
    @FXML private TableColumn<Referral, String> fromDoctorCol;
    @FXML private TableColumn<Referral, String> toDoctorCol;
    @FXML private TableColumn<Referral, String> reasonCol;
    @FXML private TableColumn<Referral, String> statusCol;
    @FXML private TableColumn<Referral, Void>   changeStatusCol;
    @FXML private TableColumn<Referral, String> dateCol;
    @FXML private TableColumn<Referral, Void>   actionsCol;

    private Consumer<Referral> onChangeStatus;

    public void setOnChangeStatus(Consumer<Referral> onChangeStatus) {
        this.onChangeStatus = onChangeStatus;
    }

    @Override
    protected void configureColumns() {
        referralIdCol.setCellValueFactory(new PropertyValueFactory<>("referralId"));
        fromDoctorCol.setCellValueFactory(new PropertyValueFactory<>("referringDoctorId"));
        toDoctorCol.setCellValueFactory(new PropertyValueFactory<>("referredToDoctorId"));
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        wireSingleActionColumn(changeStatusCol, "fas-flag",
                item -> { if (onChangeStatus != null) onChangeStatus.accept(item); });
        dateCol.setCellValueFactory(cell -> {
            var createdAt = cell.getValue().getCreatedAt();
            return new SimpleStringProperty(createdAt == null ? "" : createdAt.format(DATE_FORMAT));
        });
        wireActionsColumn(actionsCol);
    }

    @Override
    protected boolean matches(Referral referral, String lowerQuery) {
        String reason = referral.getReason();
        String status = referral.getStatus();
        return (reason != null && reason.toLowerCase().contains(lowerQuery))
                || (status != null && status.toLowerCase().contains(lowerQuery));
    }
}
