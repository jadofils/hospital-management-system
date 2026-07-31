package hospital.management.backend.dto.lab;

public class CreateLabOrderDTO {

    private String appointmentId;
    private String doctorId;
    private String testName;

    public CreateLabOrderDTO() {}

    public CreateLabOrderDTO(String appointmentId, String doctorId, String testName) {
        this.appointmentId = appointmentId;
        this.doctorId      = doctorId;
        this.testName      = testName;
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    @Override
    public String toString() {
        return "CreateLabOrderDTO{testName='" + testName + "'}";
    }
}