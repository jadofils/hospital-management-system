package hospital.management.model;

import javafx.beans.property.*;

public class Doctor {
    private final StringProperty id;
    private final StringProperty name;
    private final StringProperty specialty;
    private final StringProperty phone;
    private final StringProperty email;
    private final BooleanProperty available;

    public Doctor(String id, String name, String specialty, String phone, String email, boolean available) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.specialty = new SimpleStringProperty(specialty);
        this.phone = new SimpleStringProperty(phone);
        this.email = new SimpleStringProperty(email);
        this.available = new SimpleBooleanProperty(available);
    }

    public String getId() { return id.get(); }
    public StringProperty idProperty() { return id; }
    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public String getSpecialty() { return specialty.get(); }
    public StringProperty specialtyProperty() { return specialty; }
    public String getPhone() { return phone.get(); }
    public StringProperty phoneProperty() { return phone; }
    public String getEmail() { return email.get(); }
    public StringProperty emailProperty() { return email; }
    public boolean isAvailable() { return available.get(); }
    public BooleanProperty availableProperty() { return available; }
}