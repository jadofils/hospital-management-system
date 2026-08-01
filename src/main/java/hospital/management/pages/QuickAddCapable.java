package hospital.management.pages;

/**
 * Implemented by page controllers whose primary "Add" dialog can be opened
 * immediately after navigating in — lets Dashboard's quick-action buttons
 * jump straight to the right page and pop the form, instead of just landing
 * on a blank list.
 */
public interface QuickAddCapable {
    void openAddDialog();
}
