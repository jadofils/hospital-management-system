package hospital.management.pages.components.patient;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.patient.PatientAllergy;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class PatientAllergyTableController extends PaginatedTableController<PatientAllergy> {

    @FXML private TableColumn<PatientAllergy, String> allergenColumn;
    @FXML private TableColumn<PatientAllergy, String> reactionColumn;
    @FXML private TableColumn<PatientAllergy, String> severityColumn;
    @FXML private TableColumn<PatientAllergy, Void>    actionsColumn;

    @Override
    protected void configureColumns() {
        allergenColumn.setCellValueFactory(new PropertyValueFactory<>("allergen"));
        reactionColumn.setCellValueFactory(new PropertyValueFactory<>("reaction"));
        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(PatientAllergy allergy, String lowerQuery) {
        String allergen = allergy.getAllergen();
        String reaction = allergy.getReaction();
        return (allergen != null && allergen.toLowerCase().contains(lowerQuery))
                || (reaction != null && reaction.toLowerCase().contains(lowerQuery));
    }
}
