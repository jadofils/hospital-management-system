package hospital.management.pages.utils;

import hospital.management.backend.dto.auth.PermissionDTO;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Renders a role's permissions grouped by resource as the same visual card grid used
 *  on the Permission tab, instead of one long flat comma-joined string. */
public final class PermissionDisplayFormatter {

    private PermissionDisplayFormatter() {}

    /** Builds the same resource-grouped card grid used on the Permission tab
     *  ({@code PermissionCardsController}), but read-only — used by View Role so a
     *  role's permissions are shown visually instead of as a text field. */
    public static VBox buildCards(List<PermissionDTO> permissions) {
        VBox cardsBox = new VBox(12);
        if (permissions == null || permissions.isEmpty()) {
            Label empty = new Label("No permissions assigned.");
            cardsBox.getChildren().add(empty);
            return cardsBox;
        }

        Map<String, List<PermissionDTO>> byResource = permissions.stream()
                .collect(Collectors.groupingBy(PermissionDTO::getResource, TreeMap::new, Collectors.toList()));

        for (Map.Entry<String, List<PermissionDTO>> entry : byResource.entrySet()) {
            Label resourceLabel = new Label(capitalize(entry.getKey()));
            resourceLabel.getStyleClass().add("permission-resource-label");

            FlowPane actionsRow = new FlowPane(12, 8);
            entry.getValue().stream()
                    .map(PermissionDTO::getAction)
                    .sorted()
                    .forEach(action -> {
                        HBox chip = new HBox(8);
                        chip.getStyleClass().add("permission-chip");
                        Label actionLabel = new Label(action);
                        actionLabel.getStyleClass().add("permission-chip-label");
                        chip.getChildren().add(actionLabel);
                        actionsRow.getChildren().add(chip);
                    });

            VBox card = new VBox(8, resourceLabel, actionsRow);
            card.getStyleClass().add("permission-resource-card");
            cardsBox.getChildren().add(card);
        }
        return cardsBox;
    }

    private static String capitalize(String resource) {
        if (resource == null || resource.isBlank()) return "Unknown";
        String normalized = resource.replace('_', ' ');
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }
}
