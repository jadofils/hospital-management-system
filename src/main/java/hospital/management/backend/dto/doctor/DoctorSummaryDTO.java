package hospital.management.backend.dto.doctor;

public class DoctorSummaryDTO {

    private String doctorId;
    private String fullName;
    private String specialization;
    private String departmentName;

    public DoctorSummaryDTO() {}

    public DoctorSummaryDTO(String doctorId, String fullName,
                            String specialization, String departmentName) {
        this.doctorId       = doctorId;
        this.fullName       = fullName;
        this.specialization = specialization;
        this.departmentName = departmentName;
    }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    @Override
    public String toString() {
        return "DoctorSummaryDTO{doctorId='" + doctorId + "', fullName='" + fullName + "'}";
    }
}