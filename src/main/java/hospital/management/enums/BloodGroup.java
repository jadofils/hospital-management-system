package hospital.management.enums;

/** Blood group values used in the patient registration form. */
public enum BloodGroup {
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-");

    private final String label;

    BloodGroup(String label) { this.label = label; }

    public String getLabel() { return label; }

    public static BloodGroup fromLabel(String label) {
        for (BloodGroup g : values()) {
            if (g.label.equalsIgnoreCase(label)) return g;
        }
        throw new IllegalArgumentException("Unknown BloodGroup: " + label);
    }

    @Override public String toString() { return label; }
}