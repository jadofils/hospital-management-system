package hospital.management.backend.model.base;

/**
 * Abstract base for any entity that represents a human being with
 * contact details — currently Patient and Doctor.
 *
 * Polymorphism: getSummary() is implemented here using getDisplayTitle(),
 * which each subclass defines differently ("Patient", "Dr.", etc.).
 * Calling getSummary() on any Person subtype produces the correct output
 * without the caller knowing the concrete type.
 */
public abstract class Person extends BaseEntity {

    private String firstName;
    private String lastName;
    private String phone;
    private String email;

    protected Person() {}

    protected Person(String id, String firstName, String lastName, String phone, String email) {
        super(id);
        this.firstName = firstName;
        this.lastName  = lastName;
        this.phone     = phone;
        this.email     = email;
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    /** Title prefix for display — subclass returns "Patient", "Dr.", etc. */
    public abstract String getDisplayTitle();

    /** Polymorphic: "Dr. Jane Smith" or "Patient: John Doe" depending on subtype. */
    @Override
    public String getSummary() {
        return getDisplayTitle() + " " + getFullName() + (email != null ? " | " + email : "");
    }

    // ── Contact details ───────────────────────────────────────────────────────

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    /** Convenience: "John Doe" — never null, may be empty if names not set. */
    public String getFullName() {
        String f = firstName != null ? firstName : "";
        String l = lastName  != null ? lastName  : "";
        return (f + " " + l).strip();
    }
}