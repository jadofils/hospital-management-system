package hospital.management.pages.components;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class ButtonController {
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Button deleteBtn;

    public void initialize(){
        saveBtn.setOnAction(e->System.out.println("Save Clicked"));
        cancelBtn.setOnAction(e->System.out.println("Cancel clicked"));
        deleteBtn.setOnAction(e->System.out.println("Delete clicked"));

    }
}
