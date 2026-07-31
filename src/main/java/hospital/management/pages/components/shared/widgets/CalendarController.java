package hospital.management.pages.components.shared.widgets;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;

public class CalendarController {

    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;

    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private Consumer<LocalDate> onDateSelected;

    public void initialize() {
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        renderMonth();
    }

    public void setOnDateSelected(Consumer<LocalDate> callback) {
        this.onDateSelected = callback;
    }

    @FXML
    private void handlePrev() {
        currentMonth = currentMonth.minusMonths(1);
        renderMonth();
    }

    @FXML
    private void handleNext() {
        currentMonth = currentMonth.plusMonths(1);
        renderMonth();
    }

    private void renderMonth() {
        calendarGrid.getChildren().clear();
        monthYearLabel.setText(currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                + " " + currentMonth.getYear());

        for (DayOfWeek day : DayOfWeek.values()) {
            Label header = new Label(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            header.getStyleClass().add("calendar-day-header");
            calendarGrid.add(header, day.getValue() - 1, 0);
        }

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int startColumn = firstOfMonth.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentMonth.lengthOfMonth();

        int row = 1;
        int col = startColumn;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            ToggleButton dayBtn = new ToggleButton(String.valueOf(day));
            dayBtn.getStyleClass().add("calendar-day-btn");
            if (date.equals(selectedDate)) {
                dayBtn.getStyleClass().add("selected");
            }
            dayBtn.setOnAction(e -> selectDate(date));
            calendarGrid.add(dayBtn, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private void selectDate(LocalDate date) {
        selectedDate = date;
        renderMonth();
        if (onDateSelected != null) {
            onDateSelected.accept(date);
        }
    }
}
