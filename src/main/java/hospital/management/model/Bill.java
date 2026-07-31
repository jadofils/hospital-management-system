package hospital.management.model;

import javafx.beans.property.*;

public class Bill {
    private final StringProperty id;
    private final StringProperty patientName;
    private final StringProperty date;
    private final DoubleProperty amount;
    private final StringProperty status;
    private final StringProperty description;

    public Bill(String id, String patientName, String date,
                double amount, String status, String description) {
        this.id = new SimpleStringProperty(id);
        this.patientName = new SimpleStringProperty(patientName);
        this.date = new SimpleStringProperty(date);
        this.amount = new SimpleDoubleProperty(amount);
        this.status = new SimpleStringProperty(status);
        this.description = new SimpleStringProperty(description);
    }

    public String getId() { return id.get(); }
    public StringProperty idProperty() { return id; }
    public String getPatientName() { return patientName.get(); }
    public StringProperty patientNameProperty() { return patientName; }
    public String getDate() { return date.get(); }
    public StringProperty dateProperty() { return date; }
    public double getAmount() { return amount.get(); }
    public DoubleProperty amountProperty() { return amount; }
    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public String getDescription() { return description.get(); }
    public StringProperty descriptionProperty() { return description; }
}