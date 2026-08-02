package hospital.management.pages.components.shared.widgets;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class StatsWidgetController {

    @FXML private Label totalPatientsValue;
    @FXML private Label totalPatientsTrend;
    @FXML private Label todayAppointmentsValue;
    @FXML private Label todayAppointmentsTrend;
    @FXML private Label revenueValue;
    @FXML private Label revenueTrend;
    @FXML private Label pendingBillsValue;
    @FXML private Label pendingBillsTrend;

    public void initialize() {
        setStats(0, "—", 0, "—", 0, "—", 0, "—");
    }

    public void setStats(int totalPatients, String patientsTrend,
                          int todayAppointments, String appointmentsTrend,
                          double revenue, String revenueTrendText,
                          int pendingBills, String pendingTrend) {
        totalPatientsValue.setText(String.valueOf(totalPatients));
        totalPatientsTrend.setText(patientsTrend);
        todayAppointmentsValue.setText(String.valueOf(todayAppointments));
        todayAppointmentsTrend.setText(appointmentsTrend);
        revenueValue.setText(String.format("$%,.1fk", revenue / 1000));
        revenueTrend.setText(revenueTrendText);
        pendingBillsValue.setText(String.valueOf(pendingBills));
        pendingBillsTrend.setText(pendingTrend);
    }
}
