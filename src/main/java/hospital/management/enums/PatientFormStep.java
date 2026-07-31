package hospital.management.enums;

/**
 * The three steps of the patient registration form.
 * step  — 1-based index used for step visibility logic
 * label — displayed in the step indicator row
 */
public enum PatientFormStep {
    PERSONAL_INFO(1, "Personal Info"),
    CONTACT_INFO(2,  "Contact & Address"),
    MEDICAL_INFO(3,  "Medical Details");

    private final int    step;
    private final String label;

    PatientFormStep(int step, String label) {
        this.step  = step;
        this.label = label;
    }

    public int    getStep()  { return step; }
    public String getLabel() { return label; }

    public PatientFormStep previous() {
        for (PatientFormStep s : values()) {
            if (s.step == this.step - 1) return s;
        }
        return this;
    }

    public PatientFormStep next() {
        for (PatientFormStep s : values()) {
            if (s.step == this.step + 1) return s;
        }
        return this;
    }

    public boolean isFirst() { return step == 1; }
    public boolean isLast()  { return step == values().length; }

    public static PatientFormStep fromStep(int step) {
        for (PatientFormStep s : values()) {
            if (s.step == step) return s;
        }
        throw new IllegalArgumentException("Unknown PatientFormStep: " + step);
    }

    @Override public String toString() { return label; }
}