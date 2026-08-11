package hospital.management.pages.components.auth;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.auth.UserSessionDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class UserSessionTableController extends PaginatedTableController<UserSessionDTO> {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @FXML private TableColumn<UserSessionDTO, String> sessionLoginCol;
    @FXML private TableColumn<UserSessionDTO, String> sessionExpiryCol;
    @FXML private TableColumn<UserSessionDTO, String> sessionIpCol;
    @FXML private TableColumn<UserSessionDTO, String> sessionAgentCol;
    @FXML private TableColumn<UserSessionDTO, Void>   sessionActionCol;

    private Consumer<UserSessionDTO> onRevoke;

    /** Registers the callback invoked when the row-level Revoke button is pressed. */
    public void setOnRevoke(Consumer<UserSessionDTO> onRevoke) {
        this.onRevoke = onRevoke;
    }

    @Override
    protected void configureColumns() {
        sessionLoginCol.setCellValueFactory(cell -> new SimpleStringProperty(formatDateTime(cell.getValue().getLoginAt())));
        sessionExpiryCol.setCellValueFactory(cell -> new SimpleStringProperty(formatDateTime(cell.getValue().getExpiresAt())));
        sessionIpCol.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        sessionAgentCol.setCellValueFactory(new PropertyValueFactory<>("userAgent"));
        addSortOption("Login", sessionLoginCol);
        addSortOption("Expiry", sessionExpiryCol);
        addSortOption("IP", sessionIpCol);
        addSortOption("User Agent", sessionAgentCol);
        wireRevokeColumn(sessionActionCol);
    }

    @Override
    protected boolean matches(UserSessionDTO session, String lowerQuery) {
        return true;
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMAT);
    }

    /**
     * Renders a single "Revoke" row-action button, driven by {@link #onRevoke}.
     * Mirrors the visual style of {@link PaginatedTableController#wireActionsColumn}
     * but only needs one button, not an edit/delete pair.
     */
    private void wireRevokeColumn(TableColumn<UserSessionDTO, Void> actionsColumn) {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button revokeBtn = new Button("", new FontIcon("fas-sign-out-alt"));
            {
                revokeBtn.getStyleClass().addAll("row-action-btn", "danger");
                revokeBtn.setOnAction(e -> {
                    if (onRevoke != null) onRevoke.accept(getTableView().getItems().get(getIndex()));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : revokeBtn);
            }
        });
    }
}
