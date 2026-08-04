package hospital.management.pages.components.shared.widgets;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;

import java.time.LocalTime;

/**
 * Hour/minute spinner pair standing in for free-text "HH:mm" time entry, so forms can no
 * longer receive an unparsable or malformed time value — the spinners only ever hold valid
 * 0-23 / 0-59 values. Passed as a single field to {@code FormDialogController.addField(...)}
 * (which accepts any Node).
 */
public class TimeField extends HBox {

    private final Spinner<Integer> hourSpinner = new Spinner<>();
    private final Spinner<Integer> minuteSpinner = new Spinner<>();

    public TimeField() {
        super(4);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("time-field");

        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5));
        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);
        hourSpinner.getStyleClass().add("time-field-spinner");
        minuteSpinner.getStyleClass().add("time-field-spinner");
        hourSpinner.setPrefWidth(72);
        minuteSpinner.setPrefWidth(72);

        Label separator = new Label(":");
        separator.getStyleClass().add("time-field-separator");

        getChildren().addAll(hourSpinner, separator, minuteSpinner);
    }

    public LocalTime getTime() {
        return LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
    }

    public void setTime(LocalTime time) {
        if (time == null) return;
        hourSpinner.getValueFactory().setValue(time.getHour());
        minuteSpinner.getValueFactory().setValue(time.getMinute());
    }
}
