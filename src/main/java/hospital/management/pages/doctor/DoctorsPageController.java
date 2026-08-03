package hospital.management.pages.doctor;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dto.doctor.CreateDoctorDTO;
import hospital.management.backend.dto.doctor.DoctorDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.department.interfaces.DoctorService;
import hospital.management.backend.service.department.DepartmentServiceImpl;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.doctor.DoctorTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DoctorsPageController extends BasePageController {

    private static final int FETCH_SIZE = 500;

    private final DepartmentServiceImpl departmentService = new DepartmentServiceImpl(new DepartmentDAOImpl());
    private final DoctorService doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private DoctorTableController doctorTableController;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private Button addDoctorBtn;
    @FXML private Label totalLabel;

    private List<DoctorDTO> doctors = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.DOCTORS);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        departmentFilter.setOnAction(e -> applyFilter());

        addDoctorBtn.setOnAction(e -> openDoctorDialog(null));
        doctorTableController.setRowActions(this::openDoctorDialog, this::confirmDeleteDoctor, this::viewDoctorDetail);

        refreshTable();
    }

    private void applyFilter() {
        doctorTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        try {
            doctors = doctorService.findAll(CursorPagination.firstPage(FETCH_SIZE)).getItems();
            doctorTableController.setItems(doctors);
            totalLabel.setText("Total: " + doctors.size() + " doctors");
        } catch (Exception e) {
            toastError("Failed to load doctors: " + e.getMessage());
        }
    }

    private void viewDoctorDetail(DoctorDTO doctor) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Full Name", doctor.getFullName());
        fields.put("Specialization", doctor.getSpecialization());
        try {
            fields.put("Department", entityLookupService.departmentLabel(doctor.getDepartmentId()));
        } catch (Exception ex) {
            fields.put("Department", "Unknown");
        }
        fields.put("Phone", doctor.getPhone());
        fields.put("Email", doctor.getEmail());
        detailViewController.show("Doctor Details", "fas-user-md", fields);
    }

    private void confirmDeleteDoctor(DoctorDTO doctor) {
        confirm("Delete Doctor",
                "Are you sure you want to delete " + doctor.getFullName() + "? This cannot be undone.",
                () -> {
                    try {
                        doctorService.delete(doctor.getDoctorId());
                        refreshTable();
                        toastSuccess("Doctor deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete doctor: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog in Add mode (doctor == null) or Update mode. */
    private void openDoctorDialog(DoctorDTO doctor) {
        boolean addMode = doctor == null;

        TextField firstName = new TextField();
        TextField lastName = new TextField();
        TextField specialization = new TextField();
        LoadingIdComboBox departmentIdField = new LoadingIdComboBox();
        EntityIdComboBox departmentId = departmentIdField.getComboBox();
        TextField phone = new TextField();
        TextField email = new TextField();

        List.of(firstName, lastName, specialization, phone, email)
                .forEach(f -> f.getStyleClass().add("form-input"));
        departmentId.getStyleClass().add("form-combo");

        List<Control> otherFields = List.of(firstName, lastName, specialization, phone, email);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            firstName.setText(doctor.getFirstName());
            lastName.setText(doctor.getLastName());
            specialization.setText(doctor.getSpecialization());
            phone.setText(doctor.getPhone());
            email.setText(doctor.getEmail());
        }

        formDialogController.open(addMode ? "Add Doctor" : "Update Doctor", "fas-user-md", addMode, v -> {
            String fn = firstName.getText() == null ? "" : firstName.getText().trim();
            String ln = lastName.getText() == null ? "" : lastName.getText().trim();
            if (fn.isEmpty() || ln.isEmpty()) {
                formDialogController.setError("First name and last name are required.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                CreateDoctorDTO dto = new CreateDoctorDTO(
                        departmentId.getSelectedId(), fn, ln,
                        specialization.getText(), phone.getText(), email.getText());
                if (addMode) {
                    doctorService.create(dto);
                } else {
                    doctorService.update(doctor.getDoctorId(), dto);
                }
                refreshTable();
                formDialogController.close();
                toastSuccess(addMode ? "Doctor added." : "Doctor updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save doctor: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("First Name", "fas-user", firstName);
        formDialogController.addField("Last Name", "fas-user", lastName);
        formDialogController.addField("Specialization", "fas-stethoscope", specialization);
        formDialogController.addField("Department", "fas-hospital", departmentIdField);
        formDialogController.addField("Phone", "fas-phone", phone);
        formDialogController.addField("Email", "fas-envelope", email);

        loadDepartmentDropdown(departmentIdField, otherFields, addMode ? null : doctor);
    }

    /** Loads the department dropdown options asynchronously, showing its own spinner while
     *  data is in flight and keeping the rest of the form disabled until it finishes loading. */
    private void loadDepartmentDropdown(LoadingIdComboBox departmentIdField, List<Control> otherFields, DoctorDTO existing) {
        EntityIdComboBox departmentId = departmentIdField.getComboBox();

        departmentIdField.setLoading(true);
        formDialogController.setLoading(true);

        AsyncJobRunner.submit(
            departmentService::findAll,
            items -> {
                departmentId.setOptions(items.stream()
                        .map(d -> new EntityIdComboBox.Option(d.getDepartmentId(), d.getName()))
                        .toList());
                if (existing != null) departmentId.selectById(existing.getDepartmentId());
                departmentIdField.setLoading(false);
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            },
            ex -> {
                departmentIdField.setLoading(false);
                toastError("Failed to load departments: " + ex.getMessage());
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            });
    }
}
