package hospital.management.pages.components.doctor;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.doctor.ReferralDTO;
import hospital.management.backend.service.lookup.EntityLookupService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ReferralTableController extends PaginatedTableController<ReferralDTO> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EntityLookupService lookupService = new EntityLookupService();

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
        referralIdCol.setVisible(false);
        fromDoctorCol.setText("From Doctor");
        fromDoctorCol.setCellValueFactory(cell ->
                new SimpleStringProperty(resolveLabel(() -> lookupService.doctorLabel(cell.getValue().getReferringDoctorId()))));
        toDoctorCol.setText("To Doctor");
        toDoctorCol.setCellValueFactory(cell ->
                new SimpleStringProperty(resolveLabel(() -> lookupService.doctorLabel(cell.getValue().getReferredToDoctorId()))));
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        wireSingleActionColumn(changeStatusCol, "fas-flag",
                item -> { if (onChangeStatus != null) onChangeStatus.accept(item); });
        dateCol.setCellValueFactory(cell -> {
            var createdAt = cell.getValue().getCreatedAt();
            return new SimpleStringProperty(createdAt == null ? "" : createdAt.format(DATE_FORMAT));
        });
        addSortOption("From Doctor", fromDoctorCol);
        addSortOption("To Doctor", toDoctorCol);
        addSortOption("Reason", reasonCol);
        addSortOption("Status", statusCol);
        addSortOption("Date", dateCol);
        wireActionsColumn(actionsCol);
    }

    @Override
    protected boolean matches(ReferralDTO referral, String lowerQuery) {
        String dateStr = referral.getCreatedAt() != null ? referral.getCreatedAt().format(DATE_FORMAT) : "";
        return safe(referral.getReason()).contains(lowerQuery)
            || safe(referral.getStatus()).contains(lowerQuery)
            || safe(referral.getReferringDoctorId()).contains(lowerQuery)
            || safe(referral.getReferredToDoctorId()).contains(lowerQuery)
            || dateStr.contains(lowerQuery);
    }

    private static String safe(String s) { return s == null ? "" : s.toLowerCase(); }
}
