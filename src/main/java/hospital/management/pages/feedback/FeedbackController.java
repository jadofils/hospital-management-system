package hospital.management.pages.feedback;

import hospital.management.backend.dao.patient.PatientFeedbackDAOImpl;
import hospital.management.backend.dto.patient.PatientFeedbackDTO;
import hospital.management.backend.service.patient.FeedbackServiceImpl;
import hospital.management.backend.service.patient.interfaces.FeedbackService;
import hospital.management.enums.PageRoute;
import hospital.management.pages.BasePageController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;

public class FeedbackController extends BasePageController {

    private final FeedbackService feedbackService = new FeedbackServiceImpl(new PatientFeedbackDAOImpl());

    @FXML private TextField searchField;
    @FXML private Button refreshBtn;
    @FXML private TableView<PatientFeedbackDTO> feedbackTable;
    @FXML private TableColumn<PatientFeedbackDTO, String> feedbackIdColumn;
    @FXML private TableColumn<PatientFeedbackDTO, String> patientIdColumn;
    @FXML private TableColumn<PatientFeedbackDTO, String> appointmentIdColumn;
    @FXML private TableColumn<PatientFeedbackDTO, Integer> ratingColumn;
    @FXML private TableColumn<PatientFeedbackDTO, String> commentsColumn;
    @FXML private TableColumn<PatientFeedbackDTO, java.time.LocalDate> dateSubmittedColumn;
    @FXML private javafx.scene.control.Label totalFeedbackLabel;
    @FXML private javafx.scene.control.Label averageRatingLabel;
    @FXML private javafx.scene.control.Label fiveStarLabel;

    private final ObservableList<PatientFeedbackDTO> feedbackItems = FXCollections.observableArrayList();
    private FilteredList<PatientFeedbackDTO> filteredFeedback;

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.FEEDBACK);

        feedbackIdColumn.setCellValueFactory(new PropertyValueFactory<>("feedbackId"));
        patientIdColumn.setCellValueFactory(new PropertyValueFactory<>("patientId"));
        appointmentIdColumn.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));
        commentsColumn.setCellValueFactory(new PropertyValueFactory<>("comments"));
        dateSubmittedColumn.setCellValueFactory(new PropertyValueFactory<>("dateSubmitted"));

        filteredFeedback = new FilteredList<>(feedbackItems, f -> true);
        feedbackTable.setItems(filteredFeedback);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        refreshBtn.setOnAction(e -> refreshFeedback());

        refreshFeedback();
    }

    private void refreshFeedback() {
        try {
            feedbackItems.setAll(feedbackService.findAll());
            filteredFeedback.setPredicate(f -> true);
            applyFilter();
            updateSummary();
        } catch (Exception e) {
            toastError("Failed to load patient feedback: " + e.getMessage());
        }
    }

    private void updateSummary() {
        int total = feedbackItems.size();
        double average = feedbackItems.stream()
                .filter(f -> f.getRating() != null)
                .mapToInt(PatientFeedbackDTO::getRating)
                .average()
                .orElse(0.0);
        long fiveStar = feedbackItems.stream()
                .filter(f -> Integer.valueOf(5).equals(f.getRating()))
                .count();

        totalFeedbackLabel.setText(String.valueOf(total));
        averageRatingLabel.setText(String.format("%.1f", average));
        fiveStarLabel.setText(String.valueOf(fiveStar));
    }

    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        filteredFeedback.setPredicate(item -> {
            if (query.isBlank()) return true;
            return (item.getFeedbackId() != null && item.getFeedbackId().toLowerCase().contains(query))
                    || (item.getPatientId() != null && item.getPatientId().toLowerCase().contains(query))
                    || (item.getAppointmentId() != null && item.getAppointmentId().toLowerCase().contains(query))
                    || (item.getComments() != null && item.getComments().toLowerCase().contains(query))
                    || (item.getRating() != null && String.valueOf(item.getRating()).contains(query));
        });
    }
}
