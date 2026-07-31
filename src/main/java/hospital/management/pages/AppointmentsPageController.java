package hospital.management.pages;

import hospital.management.pages.components.CalendarController;
import hospital.management.pages.components.SidebarController;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.enums.PageRoute;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;

public class AppointmentsPageController {

    @FXML private SidebarController sidebarController;
    @FXML private CalendarController calendarController;

    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> apptIdCol;
    @FXML private TableColumn<Appointment, String> apptPatientCol;
    @FXML private TableColumn<Appointment, String> apptDoctorCol;
    @FXML private TableColumn<Appointment, String> apptTimeCol;
    @FXML private TableColumn<Appointment, String> apptStatusCol;

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.APPOINTMENTS);

        apptIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        apptPatientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        apptDoctorCol.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        apptTimeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        apptStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        appointmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        if (calendarController != null) {
            calendarController.setOnDateSelected(this::loadAppointmentsForDate);
        }

        loadAppointmentsForDate(LocalDate.now());
    }

    private void loadAppointmentsForDate(LocalDate date) {
        appointmentsTable.setItems(FXCollections.observableArrayList(
            new Appointment("A001", "Alice Johnson", "Dr. Smith",    date.toString(), "09:00", "Confirmed", ""),
            new Appointment("A002", "Bob Smith",     "Dr. Williams", date.toString(), "10:30", "Pending",   ""),
            new Appointment("A003", "Clara Davis",   "Dr. Johnson",  date.toString(), "14:00", "Confirmed", ""),
            new Appointment("A004", "Daniel Brown",  "Dr. Brown",    date.toString(), "15:30", "Cancelled", "")
        ));
    }
}