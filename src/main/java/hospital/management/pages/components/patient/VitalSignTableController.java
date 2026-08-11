package hospital.management.pages.components.patient;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.patient.VitalSignDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class VitalSignTableController extends PaginatedTableController<VitalSignDTO> {

    private static final DateTimeFormatter RECORDED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<VitalSignDTO, String>     recordedAtColumn;
    @FXML private TableColumn<VitalSignDTO, Integer>    heartRateColumn;
    @FXML private TableColumn<VitalSignDTO, BigDecimal> temperatureColumn;
    @FXML private TableColumn<VitalSignDTO, BigDecimal> weightColumn;
    @FXML private TableColumn<VitalSignDTO, BigDecimal> heightColumn;
    @FXML private TableColumn<VitalSignDTO, Void>       actionsColumn;

    @Override
    protected void configureColumns() {
        recordedAtColumn.setCellValueFactory(cell -> {
            var recordedAt = cell.getValue().getRecordedAt();
            return new SimpleStringProperty(recordedAt == null ? "" : recordedAt.format(RECORDED_AT_FORMAT));
        });
        heartRateColumn.setCellValueFactory(new PropertyValueFactory<>("heartRate"));
        temperatureColumn.setCellValueFactory(new PropertyValueFactory<>("temperatureCelsius"));
        weightColumn.setCellValueFactory(new PropertyValueFactory<>("weightKg"));
        heightColumn.setCellValueFactory(new PropertyValueFactory<>("heightCm"));
        addSortOption("Recorded", recordedAtColumn);
        addSortOption("Heart Rate", heartRateColumn);
        addSortOption("Temperature", temperatureColumn);
        addSortOption("Weight", weightColumn);
        addSortOption("Height", heightColumn);
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(VitalSignDTO vital, String lowerQuery) {
        String recordedAt = vital.getRecordedAt() != null ? vital.getRecordedAt().format(RECORDED_AT_FORMAT) : "";
        String heartRate = vital.getHeartRate() != null ? vital.getHeartRate().toString() : "";
        return recordedAt.toLowerCase().contains(lowerQuery) || heartRate.contains(lowerQuery);
    }
}
