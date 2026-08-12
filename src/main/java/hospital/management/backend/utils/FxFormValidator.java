package hospital.management.backend.utils;

import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * Attaches real-time validation listeners to JavaFX form controls.
 *
 * Each {@code attach*()} method binds a listener so validation runs on every
 * keystroke (textProperty change or value change). CSS classes {@code input-error}
 * and {@code input-valid} are applied to give immediate visual feedback.
 *
 * Error labels are optional — pass {@code null} to skip them.
 */
public final class FxFormValidator {

    public static final String ERROR_CLASS = "input-error";
    public static final String VALID_CLASS = "input-valid";

    private FxFormValidator() {}

    // ── Style helpers ─────────────────────────────────────────────────────────

    /** Marks a control as valid (green) or invalid (red) via CSS. */
    public static void applyStyle(Control control, boolean valid) {
        if (control == null) return;
        control.getStyleClass().removeAll(ERROR_CLASS, VALID_CLASS);
        control.getStyleClass().add(valid ? VALID_CLASS : ERROR_CLASS);
    }

    /** Removes all validation CSS from a control (neutral / untouched state). */
    public static void clearStyle(Control control) {
        if (control == null) return;
        control.getStyleClass().removeAll(ERROR_CLASS, VALID_CLASS);
    }

    /** Returns {@code true} if the control currently has no error class. */
    public static boolean isValid(Control control) {
        return control != null && !control.getStyleClass().contains(ERROR_CLASS);
    }

    /** Returns {@code true} if ANY of the supplied controls carries an error class. */
    public static boolean hasErrors(Control... controls) {
        for (Control c : controls) {
            if (c != null && c.getStyleClass().contains(ERROR_CLASS)) return true;
        }
        return false;
    }

    private static void setMsg(Label label, String text) {
        if (label == null) return;
        label.setText(text == null ? "" : text);
    }

    // ── TextField / TextArea validators ───────────────────────────────────────

    /**
     * Validates that the field is not blank on every keystroke.
     * Turns green when non-empty, red when empty.
     */
    public static void attachRequired(TextField field, Label errorLabel, String displayName) {
        if (field == null) return;
        field.textProperty().addListener((obs, old, val) -> {
            boolean ok = val != null && !val.isBlank();
            applyStyle(field, ok);
            setMsg(errorLabel, ok ? "" : displayName + " is required.");
        });
    }

    /** Same as {@link #attachRequired} but for TextArea. */
    public static void attachRequired(TextArea area, Label errorLabel, String displayName) {
        if (area == null) return;
        area.textProperty().addListener((obs, old, val) -> {
            boolean ok = val != null && !val.isBlank();
            applyStyle(area, ok);
            setMsg(errorLabel, ok ? "" : displayName + " is required.");
        });
    }

    /**
     * Validates email format on every keystroke.
     * An empty value is treated as valid (field is optional) unless you also
     * call {@link #attachRequired} on the same field.
     */
    public static void attachEmail(TextField field, Label errorLabel) {
        if (field == null) return;
        field.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                clearStyle(field);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = ValidatorUtils.isValidEmail(val.trim());
            applyStyle(field, ok);
            setMsg(errorLabel, ok ? "" : "Enter a valid email address (e.g. jane@hospital.com).");
        });
    }

    /**
     * Validates international phone format on every keystroke.
     * Empty value = neutral (field is optional unless combined with attachRequired).
     */
    public static void attachPhone(TextField field, Label errorLabel) {
        if (field == null) return;
        field.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                clearStyle(field);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = ValidatorUtils.isValidPhone(val.trim());
            applyStyle(field, ok);
            setMsg(errorLabel, ok ? "" : "Enter a valid phone number (e.g. +250 788 000 000).");
        });
    }

    /**
     * Enforces a minimum length on every keystroke.
     * Empty value = neutral.
     */
    public static void attachMinLength(TextField field, Label errorLabel, int min, String displayName) {
        if (field == null) return;
        field.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                clearStyle(field);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = val.trim().length() >= min;
            applyStyle(field, ok);
            setMsg(errorLabel, ok ? "" : displayName + " must be at least " + min + " characters.");
        });
    }

    /** Same as {@link #attachMinLength(TextField, Label, int, String)} but for TextArea. */
    public static void attachMinLength(TextArea area, Label errorLabel, int min, String displayName) {
        if (area == null) return;
        area.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                clearStyle(area);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = val.trim().length() >= min;
            applyStyle(area, ok);
            setMsg(errorLabel, ok ? "" : displayName + " must be at least " + min + " characters.");
        });
    }

    /** Validates max length on every keystroke. */
    public static void attachMaxLength(TextField field, Label errorLabel, int max, String displayName) {
        if (field == null) return;
        field.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                clearStyle(field);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = val.trim().length() <= max;
            applyStyle(field, ok);
            setMsg(errorLabel, ok ? "" : displayName + " must not exceed " + max + " characters.");
        });
    }

    /** Same as {@link #attachMaxLength(TextField, Label, int, String)} but for TextArea. */
    public static void attachMaxLength(TextArea area, Label errorLabel, int max, String displayName) {
        if (area == null) return;
        area.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                clearStyle(area);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = val.trim().length() <= max;
            applyStyle(area, ok);
            setMsg(errorLabel, ok ? "" : displayName + " must not exceed " + max + " characters.");
        });
    }

    /**
     * Validates that the value looks like a proper name (letters, spaces, hyphens,
     * apostrophes only) on every keystroke — mirrors {@code ValidatorUtils.requireValidName}'s
     * format rule so genuine proper-noun fields (person/department names) get the same
     * real-time feedback the server already enforces. Empty value = neutral.
     */
    public static void attachName(TextField field, Label errorLabel, String displayName) {
        if (field == null) return;
        field.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                clearStyle(field);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = val.trim().matches("[\\p{L}\\s'\\-]+");
            applyStyle(field, ok);
            setMsg(errorLabel, ok ? "" : displayName + " may only contain letters, spaces, hyphens, and apostrophes.");
        });
    }

    /**
     * Rejects a value that is entirely digits — for mixed-alphanumeric "name-ish" fields
     * (medication name, doctor specialization, lab test name) that legitimately contain
     * digits (e.g. "Vitamin B12") but shouldn't be pure numbers. Empty value = neutral.
     */
    public static void attachNotPureNumeric(TextField field, Label errorLabel, String displayName) {
        if (field == null) return;
        field.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                clearStyle(field);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = !val.trim().matches("\\d+");
            applyStyle(field, ok);
            setMsg(errorLabel, ok ? "" : displayName + " must be a name, not a number.");
        });
    }

    // ── PasswordField validators ──────────────────────────────────────────────

    /**
     * Shows a password-strength indicator on every keystroke.
     * Label text cycles through: Weak / Fair / Strong / Very strong.
     * CSS is applied to the label itself via {@code text-danger/text-warning/text-success}.
     */
    public static void attachPasswordStrength(PasswordField field, Label strengthLabel) {
        if (field == null) return;
        field.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isEmpty()) {
                clearStyle(field);
                setMsg(strengthLabel, "");
                return;
            }
            int score = passwordScore(val);
            boolean minOk = val.length() >= 8;
            applyStyle(field, minOk && score >= 3);
            if (strengthLabel != null) {
                strengthLabel.getStyleClass().removeAll("text-danger", "text-warning", "text-success");
                if (score <= 1) {
                    strengthLabel.setText("Weak — add uppercase, numbers and symbols.");
                    strengthLabel.getStyleClass().add("text-danger");
                } else if (score == 2) {
                    strengthLabel.setText("Fair — add a symbol or number.");
                    strengthLabel.getStyleClass().add("text-warning");
                } else if (score == 3) {
                    strengthLabel.setText("Strong.");
                    strengthLabel.getStyleClass().add("text-success");
                } else {
                    strengthLabel.setText("Very strong.");
                    strengthLabel.getStyleClass().add("text-success");
                }
            }
        });
    }

    /**
     * Validates that {@code confirmField} matches {@code sourceField} on every keystroke.
     * Also re-checks whenever the source field changes.
     */
    public static void attachPasswordMatch(PasswordField sourceField, PasswordField confirmField, Label errorLabel) {
        if (sourceField == null || confirmField == null) return;
        Runnable check = () -> {
            String src  = sourceField.getText();
            String conf = confirmField.getText();
            if (conf == null || conf.isEmpty()) {
                clearStyle(confirmField);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = src != null && src.equals(conf);
            applyStyle(confirmField, ok);
            setMsg(errorLabel, ok ? "" : "Passwords do not match.");
        };
        sourceField.textProperty().addListener((obs, old, val) -> check.run());
        confirmField.textProperty().addListener((obs, old, val) -> check.run());
    }

    // ── DatePicker validators ─────────────────────────────────────────────────

    /**
     * Validates that a required DatePicker is not null when the value changes.
     */
    public static void attachDateRequired(DatePicker picker, Label errorLabel, String displayName) {
        if (picker == null) return;
        picker.valueProperty().addListener((obs, old, val) -> {
            boolean ok = val != null;
            applyStyle(picker, ok);
            setMsg(errorLabel, ok ? "" : displayName + " is required.");
        });
    }

    /**
     * Validates that the selected date is in the past (for date-of-birth fields).
     * Empty = neutral.
     */
    public static void attachPastDate(DatePicker picker, Label errorLabel, String displayName) {
        if (picker == null) return;
        picker.valueProperty().addListener((obs, old, val) -> {
            if (val == null) {
                clearStyle(picker);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = val.isBefore(LocalDate.now());
            applyStyle(picker, ok);
            setMsg(errorLabel, ok ? "" : displayName + " must be a past date.");
        });
    }

    /**
     * Validates that the date is not in the future (e.g. appointment date may be today or later).
     * Empty = neutral.
     */
    public static void attachNotPastDate(DatePicker picker, Label errorLabel, String displayName) {
        if (picker == null) return;
        picker.valueProperty().addListener((obs, old, val) -> {
            if (val == null) {
                clearStyle(picker);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = !val.isBefore(LocalDate.now());
            applyStyle(picker, ok);
            setMsg(errorLabel, ok ? "" : displayName + " cannot be in the past.");
        });
    }

    /**
     * Validates that {@code from} is not after {@code to}.
     * Called on value changes of either picker.
     */
    public static void attachDateRange(DatePicker from, DatePicker to, Label errorLabel) {
        if (from == null || to == null) return;
        Runnable check = () -> {
            LocalDate f = from.getValue();
            LocalDate t = to.getValue();
            if (f == null || t == null) {
                clearStyle(from);
                clearStyle(to);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = !f.isAfter(t);
            applyStyle(from, ok);
            applyStyle(to, ok);
            setMsg(errorLabel, ok ? "" : "'From' date must not be after 'To' date.");
        };
        from.valueProperty().addListener((obs, old, val) -> check.run());
        to.valueProperty().addListener((obs, old, val) -> check.run());
    }

    /**
     * Blocks every past day in the picker's calendar popup so past dates are
     * physically unselectable (in addition to the red/green styling from
     * {@link #attachNotPastDate}). The user must still be able to TYPE a past
     * date, so pair with an on-submit check for a hard guarantee.
     */
    public static void disallowPastDates(DatePicker picker) {
        if (picker == null) return;
        picker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
    }

    /**
     * Disables every day after today in the picker's calendar popup, so future
     * dates are physically unselectable (in addition to the red/green styling
     * from {@link #attachNotFutureDate}). The user must still be able to TYPE a
     * future date, so pair with an on-submit check for a hard guarantee.
     */
    public static void disallowFutureDates(DatePicker picker) {
        if (picker == null) return;
        picker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
    }

    /**
     * Validates that the selected date is on or after a dynamically-resolved
     * minimum date (e.g. a prescription must not be issued before its
     * appointment date). Empty picker or null minimum = neutral.
     */
    public static void attachOnOrAfterDate(DatePicker picker, Supplier<LocalDate> minDateSupplier,
                                           Label errorLabel, String displayName) {
        if (picker == null) return;
        picker.valueProperty().addListener((obs, old, val) -> {
            LocalDate min = minDateSupplier == null ? null : minDateSupplier.get();
            if (val == null || min == null) {
                clearStyle(picker);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = !val.isBefore(min);
            applyStyle(picker, ok);
            setMsg(errorLabel, ok ? "" : displayName + " must not be before " + min + ".");
        });
    }

    /**
     * Validates that the selected date is not after today (e.g. a prescription
     * cannot be issued in the future). Empty = neutral.
     */
    public static void attachNotFutureDate(DatePicker picker, Label errorLabel, String displayName) {
        if (picker == null) return;
        picker.valueProperty().addListener((obs, old, val) -> {
            if (val == null) {
                clearStyle(picker);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = !val.isAfter(LocalDate.now());
            applyStyle(picker, ok);
            setMsg(errorLabel, ok ? "" : displayName + " cannot be in the future.");
        });
    }

    // ── ComboBox validators ───────────────────────────────────────────────────

    /** Validates that a required ComboBox has a selected value. */
    public static <T> void attachRequired(ComboBox<T> combo, Label errorLabel, String displayName) {
        if (combo == null) return;
        combo.valueProperty().addListener((obs, old, val) -> {
            boolean ok = val != null;
            applyStyle(combo, ok);
            setMsg(errorLabel, ok ? "" : displayName + " is required.");
        });
    }

    // ── Spinner validators ────────────────────────────────────────────────────

    /**
     * Validates that an integer Spinner stays within [min, max] on every value change.
     * The SpinnerValueFactory already enforces this, but this adds CSS + error label.
     */
    public static void attachSpinnerRange(Spinner<Integer> spinner, Label errorLabel,
                                          int min, int max, String displayName) {
        if (spinner == null) return;
        spinner.valueProperty().addListener((obs, old, val) -> {
            if (val == null) {
                clearStyle(spinner);
                setMsg(errorLabel, "");
                return;
            }
            boolean ok = val >= min && val <= max;
            applyStyle(spinner, ok);
            setMsg(errorLabel, ok ? "" : displayName + " must be between " + min + " and " + max + ".");
        });
        // Also commit typed text on focus loss
        spinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) spinner.increment(0);
        });
    }

    // ── Bulk helpers ──────────────────────────────────────────────────────────

    /**
     * Clears validation styling from all supplied controls.
     * Useful when re-opening a dialog in Add mode.
     */
    public static void clearAll(Control... controls) {
        for (Control c : controls) clearStyle(c);
    }

    // ── Password score ────────────────────────────────────────────────────────

    private static int passwordScore(String pw) {
        int score = 0;
        if (pw.length() >= 8)                    score++;
        if (pw.chars().anyMatch(Character::isUpperCase)) score++;
        if (pw.chars().anyMatch(Character::isLowerCase)) score++;
        if (pw.chars().anyMatch(Character::isDigit))     score++;
        if (pw.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) score++;
        return score;
    }
}