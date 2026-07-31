package hospital.management.model;

import javafx.beans.property.*;

public class Patient {
    private final StringProperty id;
    private final StringProperty name;
    private final IntegerProperty age;
    private final StringProperty status;
    private final StringProperty gender;
    private final StringProperty phone;
    private final StringProperty email;

    public Patient(String id, String name, int age, String status, String gender, String phone, String email) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.age = new SimpleIntegerProperty(age);
        this.status = new SimpleStringProperty(status);
        this.gender = new SimpleStringProperty(gender);
        this.phone = new SimpleStringProperty(phone);
        this.email = new SimpleStringProperty(email);
    }

    public String getId() { return id.get(); }
    public StringProperty idProperty() { return id; }
    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public int getAge() { return age.get(); }
    public IntegerProperty ageProperty() { return age; }
    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public String getGender() { return gender.get(); }
    public StringProperty genderProperty() { return gender; }
    public String getPhone() { return phone.get(); }
    public StringProperty phoneProperty() { return phone; }
    public String getEmail() { return email.get(); }
    public StringProperty emailProperty() { return email; }
}