package hospital.management.pages;

import hospital.management.backend.config.security.PermissionGate;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.shared.feedback.ButtonSpinner;
import hospital.management.pages.components.shared.feedback.DetailViewController;
import hospital.management.pages.components.shared.feedback.FormDialogController;
import hospital.management.pages.components.shared.feedback.ModalController;
import hospital.management.pages.components.shared.layout.RightSidebarController;
import hospital.management.pages.components.shared.layout.SidebarController;
import hospital.management.pages.components.shared.feedback.ToastController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.util.function.Consumer;

/**
 * Shared shell for every full-page controller. Every page includes the same
 * sidebar/toast/confirm-modal/form-dialog quartet the same way; this base
 * class injects them once and exposes the common feedback helpers so page
 * controllers only implement page-specific behaviour.
 */
public abstract class BasePageController {

    @FXML protected SidebarController sidebarController;
    @FXML protected RightSidebarController rightSidebarController;
    @FXML protected ToastController toastController;
    @FXML protected ModalController confirmModalController;
    @FXML protected FormDialogController formDialogController;
    @FXML protected DetailViewController detailViewController;

    protected void toast(String message, NotificationType type) {
        if (toastController != null) toastController.show(message, type);
    }

    protected void toastSuccess(String message) { toast(message, NotificationType.SUCCESS); }
    protected void toastError(String message)   { toast(message, NotificationType.ERROR); }

    /** Opens the shared confirm modal (used for destructive row actions such as delete). */
    protected void confirm(String title, String body, Runnable onConfirm) {
        if (confirmModalController != null) confirmModalController.show(title, body, onConfirm);
    }

    /**
     * Runs a standalone button action (one not already covered by the shared
     * form dialog's submit spinner or the confirm modal's confirm spinner) with
     * an in-button loading spinner. {@code action} is responsible for its own
     * toastSuccess/toastError calls; this only owns disabling/re-enabling the
     * button and showing the spinner while it runs.
     */
    protected void withSpinner(Button button, Runnable action) {
        ButtonSpinner.setLoading(button, true);
        // Deferred one pulse so the spinner graphic is guaranteed to paint
        // before the (synchronous today) action runs on the FX thread.
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                ButtonSpinner.setLoading(button, false);
            }
        });
    }

    protected boolean canCreate(PageRoute route) {
        return PermissionGate.canCreate(route);
    }

    protected boolean canUpdate(PageRoute route) {
        return PermissionGate.canUpdate(route);
    }

    protected boolean canDelete(PageRoute route) {
        return PermissionGate.canDelete(route);
    }

    protected boolean canRead(PageRoute route) {
        return PermissionGate.canRead(route);
    }

    protected void applyCreateVisibility(Button button, PageRoute route) {
        boolean allowed = canCreate(route);
        button.setVisible(allowed);
        button.setManaged(allowed);
    }

    protected <T> Consumer<T> allowUpdate(PageRoute route, Consumer<T> action) {
        return canUpdate(route) ? action : null;
    }

    protected <T> Consumer<T> allowDelete(PageRoute route, Consumer<T> action) {
        return canDelete(route) ? action : null;
    }

    protected <T> Consumer<T> allowRead(PageRoute route, Consumer<T> action) {
        return canRead(route) ? action : null;
    }
}
