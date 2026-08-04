package hospital.management.pages.components.doctor;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.doctor.DoctorDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Map;
import java.util.function.Consumer;

public class DoctorTableController extends PaginatedTableController<DoctorDTO> {

    @FXML private TableColumn<DoctorDTO, String> idColumn;
    @FXML private TableColumn<DoctorDTO, String> nameColumn;
    @FXML private TableColumn<DoctorDTO, String> specializationColumn;
    @FXML private TableColumn<DoctorDTO, String> departmentColumn;
    @FXML private TableColumn<DoctorDTO, String> roleColumn;
    @FXML private TableColumn<DoctorDTO, String> phoneColumn;
    @FXML private TableColumn<DoctorDTO, String> emailColumn;
    @FXML private TableColumn<DoctorDTO, Void>   actionsColumn;

    private Consumer<DoctorDTO> onEdit;
    private Consumer<DoctorDTO> onDelete;
    private Consumer<DoctorDTO> onViewDetails;
    private Consumer<DoctorDTO> onAssignRole;
    private Map<String, String> roleByDoctorId = Map.of();

    public void setRoleByDoctorId(Map<String, String> roleByDoctorId) {
        this.roleByDoctorId = roleByDoctorId == null ? Map.of() : roleByDoctorId;
        if (table != null) table.refresh();
    }

    public void setDoctorRowActions(Consumer<DoctorDTO> onEdit, Consumer<DoctorDTO> onDelete,
                                    Consumer<DoctorDTO> onViewDetails, Consumer<DoctorDTO> onAssignRole) {
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.onViewDetails = onViewDetails;
        this.onAssignRole = onAssignRole;
        wireDoctorActionsColumn();
    }

    @Override
    protected void configureColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        nameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getFullName()));
        specializationColumn.setCellValueFactory(new PropertyValueFactory<>("specialization"));
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("departmentId"));
        roleColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(roleByDoctorId.getOrDefault(cell.getValue().getDoctorId(), "—")));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        wireDoctorActionsColumn();
    }

    private void wireDoctorActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("", new FontIcon("fas-eye"));
            private final Button assignBtn = new Button("", new FontIcon("fas-user-tag"));
            private final Button editBtn = new Button("", new FontIcon("fas-edit"));
            private final Button deleteBtn = new Button("", new FontIcon("fas-trash"));
            private final HBox box = new HBox(4, viewBtn, assignBtn, editBtn, deleteBtn);
            {
                viewBtn.getStyleClass().add("row-action-btn");
                assignBtn.getStyleClass().add("row-action-btn");
                editBtn.getStyleClass().add("row-action-btn");
                deleteBtn.getStyleClass().addAll("row-action-btn", "danger");
                viewBtn.setOnAction(e -> {
                    if (onViewDetails != null) onViewDetails.accept(getTableView().getItems().get(getIndex()));
                });
                assignBtn.setOnAction(e -> {
                    if (onAssignRole != null) onAssignRole.accept(getTableView().getItems().get(getIndex()));
                });
                editBtn.setOnAction(e -> {
                    if (onEdit != null) onEdit.accept(getTableView().getItems().get(getIndex()));
                });
                deleteBtn.setOnAction(e -> {
                    if (onDelete != null) onDelete.accept(getTableView().getItems().get(getIndex()));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                viewBtn.setVisible(onViewDetails != null);
                viewBtn.setManaged(onViewDetails != null);
                assignBtn.setVisible(onAssignRole != null);
                assignBtn.setManaged(onAssignRole != null);
                editBtn.setVisible(onEdit != null);
                editBtn.setManaged(onEdit != null);
                deleteBtn.setVisible(onDelete != null);
                deleteBtn.setManaged(onDelete != null);
                if (onViewDetails == null && onAssignRole == null && onEdit == null && onDelete == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }

    @Override
    protected boolean matches(DoctorDTO doctor, String lowerQuery) {
        String specialization = doctor.getSpecialization();
        return doctor.getFullName().toLowerCase().contains(lowerQuery)
            || roleByDoctorId.getOrDefault(doctor.getDoctorId(), "").toLowerCase().contains(lowerQuery)
                || (specialization != null && specialization.toLowerCase().contains(lowerQuery));
    }
}
