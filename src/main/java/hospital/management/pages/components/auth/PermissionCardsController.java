package hospital.management.pages.components.auth;

import hospital.management.backend.dto.auth.PermissionDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PermissionCardsController {

    @FXML private VBox cardsBox;

    private List<PermissionDTO> all = new ArrayList<>();

    public void setItems(List<PermissionDTO> items) {
        this.all = items == null ? new ArrayList<>() : new ArrayList<>(items);
        rebuild(null);
    }

    public void filter(String q) {
        rebuild(q == null ? null : q.toLowerCase().trim());
    }

    private void rebuild(String q) {
        cardsBox.getChildren().clear();
        Map<String, List<PermissionDTO>> byResource = all.stream()
                .filter(p -> q == null || p.getResource().toLowerCase().contains(q) || p.getAction().toLowerCase().contains(q))
                .collect(Collectors.groupingBy(PermissionDTO::getResource, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<PermissionDTO>> entry : byResource.entrySet()) {
            String resource = entry.getKey();
            List<PermissionDTO> perms = entry.getValue();

            Label resourceLabel = new Label(resource);
            resourceLabel.getStyleClass().add("permission-resource-label");

            FlowPane actionsRow = new FlowPane(12, 8);
            for (PermissionDTO p : perms) {
                HBox chip = new HBox(8);
                chip.getStyleClass().add("permission-chip");
                Label action = new Label(p.getAction());
                action.getStyleClass().add("permission-chip-label");
                chip.getChildren().add(action);
                actionsRow.getChildren().add(chip);
            }

            VBox card = new VBox(8, resourceLabel, actionsRow);
            card.getStyleClass().add("permission-resource-card");
            cardsBox.getChildren().add(card);
        }
    }
}
