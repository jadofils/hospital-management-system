package hospital.management.model;

import javafx.beans.property.*;

public class Appointment {
    private final StringProperty id;
    private final StringProperty patientName;
    private final StringProperty doctorName;
    private final StringProperty date;
    private final StringProperty time;
    private final StringProperty status;
    private final StringProperty notes;

    public Appointment(String id, String patientName, String doctorName,
                       String date, String time, String status, String notes) {
        this.id = new SimpleStringProperty(id);
        this.patientName = new SimpleStringProperty(patientName);
        this.doctorName = new SimpleStringProperty(doctorName);
        this.date = new SimpleStringProperty(date);
        this.time = new SimpleStringProperty(time);
        this.status = new SimpleStringProperty(status);
        this.notes = new SimpleStringProperty(notes);
    }

    public String getId() { return id.get(); }
    public StringProperty idProperty() { return id; }
    public String getPatientName() { return patientName.get(); }
    public StringProperty patientNameProperty() { return patientName; }
    public String getDoctorName() { return doctorName.get(); }
    public StringProperty doctorNameProperty() { return doctorName; }
    public String getDate() { return date.get(); }
    public StringProperty dateProperty() { return date; }
    public String getTime() { return time.get(); }
    public StringProperty timeProperty() { return time; }
    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public String getNotes() { return notes.get(); }
    public StringProperty notesProperty() { return notes; }
}