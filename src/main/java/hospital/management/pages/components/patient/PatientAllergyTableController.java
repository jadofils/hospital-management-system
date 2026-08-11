package hospital.management.pages.components.patient;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.patient.PatientAllergyDTO;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class PatientAllergyTableController extends PaginatedTableController<PatientAllergyDTO> {

    @FXML private TableColumn<PatientAllergyDTO, String> allergenColumn;
    @FXML private TableColumn<PatientAllergyDTO, String> reactionColumn;
    @FXML private TableColumn<PatientAllergyDTO, String> severityColumn;
    @FXML private TableColumn<PatientAllergyDTO, Void>    actionsColumn;

    @Override
    protected void configureColumns() {
        allergenColumn.setCellValueFactory(new PropertyValueFactory<>("allergen"));
        reactionColumn.setCellValueFactory(new PropertyValueFactory<>("reaction"));
        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        addSortOption("Allergen", allergenColumn);
        addSortOption("Reaction", reactionColumn);
        addSortOption("Severity", severityColumn);
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(PatientAllergyDTO allergy, String lowerQuery) {
        String allergen = allergy.getAllergen();
        String reaction = allergy.getReaction();
        return (allergen != null && allergen.toLowerCase().contains(lowerQuery))
                || (reaction != null && reaction.toLowerCase().contains(lowerQuery));
    }
}
