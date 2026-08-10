package hospital.management.pages.components.doctor;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.doctor.ReferralDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ReferralTableController extends PaginatedTableController<ReferralDTO> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<ReferralDTO, String> referralIdCol;
    @FXML private TableColumn<ReferralDTO, String> fromDoctorCol;
    @FXML private TableColumn<ReferralDTO, String> toDoctorCol;
    @FXML private TableColumn<ReferralDTO, String> reasonCol;
    @FXML private TableColumn<ReferralDTO, String> statusCol;
    @FXML private TableColumn<ReferralDTO, Void>   changeStatusCol;
    @FXML private TableColumn<ReferralDTO, String> dateCol;
    @FXML private TableColumn<ReferralDTO, Void>   actionsCol;

    private Consumer<ReferralDTO> onChangeStatus;

    public void setOnChangeStatus(Consumer<ReferralDTO> onChangeStatus) {
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
    protected boolean matches(ReferralDTO referral, String lowerQuery) {
        String reason = referral.getReason();
        String status = referral.getStatus();
        return (reason != null && reason.toLowerCase().contains(lowerQuery))
                || (status != null && status.toLowerCase().contains(lowerQuery));
    }
}
