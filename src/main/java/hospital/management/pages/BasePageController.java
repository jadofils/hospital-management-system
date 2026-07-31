package hospital.management.pages;

import hospital.management.enums.NotificationType;
import hospital.management.pages.components.shared.feedback.FormDialogController;
import hospital.management.pages.components.shared.feedback.ModalController;
import hospital.management.pages.components.shared.layout.RightSidebarController;
import hospital.management.pages.components.shared.layout.SidebarController;
import hospital.management.pages.components.shared.feedback.ToastController;
import javafx.fxml.FXML;

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

    protected void toast(String message, NotificationType type) {
        if (toastController != null) toastController.show(message, type);
    }

    protected void toastSuccess(String message) { toast(message, NotificationType.SUCCESS); }
    protected void toastError(String message)   { toast(message, NotificationType.ERROR); }

    /** Opens the shared confirm modal (used for destructive row actions such as delete). */
    protected void confirm(String title, String body, Runnable onConfirm) {
        if (confirmModalController != null) confirmModalController.show(title, body, onConfirm);
    }
}
