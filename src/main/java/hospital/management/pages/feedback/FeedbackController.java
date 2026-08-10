package hospital.management.pages.feedback;

import hospital.management.backend.utils.FxFormValidator;
import hospital.management.backend.config.security.PermissionGate;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.dto.patient.CreatePatientFeedbackDTO;
import hospital.management.backend.dao.patient.PatientFeedbackDAOImpl;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.patient.PatientFeedbackDTO;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.patient.FeedbackServiceImpl;
import hospital.management.backend.service.patient.interfaces.FeedbackService;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.service.patient.interfaces.PatientService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.pages.BasePageController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;

public class FeedbackController extends BasePageController {

    private final FeedbackService feedbackService = new FeedbackServiceImpl(new PatientFeedbackDAOImpl());
    private final UserServiceImpl userService = new UserServiceImpl(new UserDAOImpl());
    private final PatientService patientService = new PatientServiceImpl(new PatientDAOImpl());

    @FXML private TextField searchField;
    @FXML private Button submitFeedbackBtn;
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
    private String resolvedPatientId;

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

        resolvedPatientId = resolveCurrentPatientId();
        boolean patientRole = isPatientRole();
        patientIdColumn.setVisible(!patientRole);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        submitFeedbackBtn.setVisible(PermissionGate.canCreate(PageRoute.FEEDBACK));
        submitFeedbackBtn.setManaged(PermissionGate.canCreate(PageRoute.FEEDBACK));
        submitFeedbackBtn.setOnAction(e -> openSubmitDialog());
        if (patientRole && resolvedPatientId == null) {
            submitFeedbackBtn.setDisable(true);
            submitFeedbackBtn.setText("Submit Feedback (Profile Link Required)");
        }
        refreshBtn.setOnAction(e -> refreshFeedback());

        refreshFeedback();
    }

    private void refreshFeedback() {
        try {
            if (isPatientRole() && resolvedPatientId != null) {
                feedbackItems.setAll(feedbackService.findByPatient(resolvedPatientId));
            } else {
                feedbackItems.setAll(feedbackService.findAll());
            }
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

    private void openSubmitDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Submit Feedback");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        TextField patientIdField = new TextField();
        TextField appointmentIdField = new TextField();
        ComboBox<Integer> ratingField = new ComboBox<>();
        TextArea commentsField = new TextArea();
        commentsField.setPrefRowCount(3);
        ratingField.getItems().addAll(1, 2, 3, 4, 5);
        ratingField.setValue(5);

        patientIdField.setPromptText("e.g. patient UUID");
        appointmentIdField.setPromptText("e.g. appointment UUID (optional)");
        commentsField.setPromptText("e.g. Very attentive staff and quick service. (required)");

        if (isPatientRole()) {
            patientIdField.setText(resolvedPatientId == null ? "" : resolvedPatientId);
            patientIdField.setDisable(true);
        } else {
            FxFormValidator.attachRequired(patientIdField, null, "Patient ID");
        }
        FxFormValidator.attachRequired(commentsField, null, "Comments");
        FxFormValidator.attachRequired(ratingField,   null, "Rating");

        grid.add(new Label("Patient ID"), 0, 0);
        grid.add(patientIdField, 1, 0);
        grid.add(new Label("Appointment ID (optional)"), 0, 1);
        grid.add(appointmentIdField, 1, 1);
        grid.add(new Label("Rating"), 0, 2);
        grid.add(ratingField, 1, 2);
        grid.add(new Label("Comments"), 0, 3);
        grid.add(commentsField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            String patientId = patientIdField.getText() == null ? "" : patientIdField.getText().trim();
            String appointmentId = appointmentIdField.getText() == null ? "" : appointmentIdField.getText().trim();
            Integer rating = ratingField.getValue();
            String comments = commentsField.getText() == null ? "" : commentsField.getText().trim();

            try {
                if (patientId.isBlank()) {
                    toastError("Patient ID is required.");
                    FxFormValidator.applyStyle(patientIdField, false);
                    return null;
                }
                if (rating == null) {
                    toastError("Rating is required.");
                    return null;
                }
                if (comments.isBlank()) {
                    toastError("Comments are required.");
                    FxFormValidator.applyStyle(commentsField, false);
                    return null;
                }
                feedbackService.submit(new CreatePatientFeedbackDTO(
                        patientId,
                        appointmentId.isBlank() ? null : appointmentId,
                        rating,
                        comments,
                        LocalDate.now()));
                refreshFeedback();
                toastSuccess("Feedback submitted.");
            } catch (Exception ex) {
                toastError("Failed to submit feedback: " + ex.getMessage());
            }
            return null;
        });

        dialog.showAndWait();
    }

    private String resolveCurrentPatientId() {
        try {
            if (!isPatientRole()) return null;

            String currentUserId = SessionManager.getCurrentUserId();
            try {
                patientService.findById(currentUserId);
                return currentUserId;
            } catch (Exception ignored) {
            }

            UserDTO currentUser = userService.findById(currentUserId);
            String email = currentUser.getEmail();
            if (email == null || email.isBlank()) return null;

            for (PatientDTO patient : patientService.findAll(CursorPagination.firstPage(1000)).getItems()) {
                if (patient.getEmail() != null && patient.getEmail().equalsIgnoreCase(email)) {
                    return patient.getPatientId();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isPatientRole() {
        try {
            return "patient".equalsIgnoreCase(SessionManager.getCurrentRole());
        } catch (Exception ignored) {
            return false;
        }
    }
}
