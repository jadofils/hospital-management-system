package hospital.management.pages.components;

import hospital.management.backend.utils.AlgorithmUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Shared base for every entity table (patients, doctors, appointments, ...).
 * Every *-table.fxml declares its fx:controller as a subclass of this, with
 * fx:id="table" and fx:id="pagination" on the TableView/Pagination.
 *
 * Subclasses only implement column binding and the search predicate; the
 * paging/filtering mechanics live here once instead of per-entity.
 *
 * <p>Sorting is delegated to {@link AlgorithmUtils#mergeSort} (O(n log n),
 * stable) on the already-filtered dataset before each page is sliced — the
 * same backend DSA the service layer uses, rather than JavaFX's built-in
 * {@code SortedList}. Clicking a column header re-sorts the full filtered
 * set via that comparator.
 */
public abstract class PaginatedTableController<T> {

    protected static final int ROWS_PER_PAGE = 10;

    @FXML protected TableView<T> table;
    @FXML protected Pagination pagination;

    protected final ObservableList<T> sourceItems = FXCollections.observableArrayList();
    protected FilteredList<T> filteredItems;

    private Consumer<T> onEdit;
    private Consumer<T> onDelete;
    private Consumer<T> onViewDetails;
    private TableColumn<T, Void> actionsColumn;

    /** Dropdown sort options registered by subclasses: label -> table column. */
    private final java.util.Map<String, TableColumn<T, ?>> sortOptions = new java.util.LinkedHashMap<>();

    /**
     * Registers a "Sort by" dropdown option backed by {@code column}. Options are
     * surfaced by the shared sort bar on each page; selecting one drives the same
     * comparator/mergeSort pipeline as clicking the column header.
     */
    protected void addSortOption(String label, TableColumn<T, ?> column) {
        sortOptions.put(label, column);
    }

    /** The labels of every registered sort option, for populating the page's sort-bar dropdown. */
    public java.util.List<String> getSortOptionLabels() {
        return List.copyOf(sortOptions.keySet());
    }

    /**
     * Applies the sort matching {@code label} in the given direction. Passing
     * {@code null} (or an unregistered label) restores natural insertion order.
     */
    public void applySort(String label, boolean ascending) {
        TableColumn<T, ?> column = label == null ? null : sortOptions.get(label);
        if (column == null) {
            clearSort();
            return;
        }
        sortByColumn(column, ascending ? TableColumn.SortType.ASCENDING : TableColumn.SortType.DESCENDING);
    }

    public void initialize() {
        configureColumns();
        filteredItems = new FilteredList<>(sourceItems, item -> true);
        // Prevent TableView from sorting its displayed page-slice in-place;
        // our mergeSort + renderPage listener already handles the correct ordering.
        table.setSortPolicy(t -> true);
        // Re-render the current page whenever the sort order changes.
        table.comparatorProperty().addListener((obs, o, n) -> renderPage(pagination.getCurrentPageIndex()));
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        pagination.currentPageIndexProperty().addListener((obs, o, n) -> renderPage(n.intValue()));
        refreshPagination();
    }

    /**
     * Registers the row-level edit/delete callbacks used by {@link #wireActionsColumn}, with no view-details action.
     */
    protected abstract void configureColumns();

    /** True if the item matches an already-trimmed, lower-cased search query. */
    protected abstract boolean matches(T item, String lowerQuery);

    /** Replaces the full backing list (e.g. after a fetch or an add/update/delete). */
    public void setItems(List<T> items) {
        sourceItems.setAll(items);
        refreshPagination();
    }

    public void filter(String query) {
        String lower = query == null ? "" : query.trim().toLowerCase();
        filteredItems.setPredicate(item -> lower.isEmpty() || matches(item, lower));
        refreshPagination();
    }

    /**
     * Applies an arbitrary predicate — useful when a page controller needs to
     * combine a text search with a secondary filter (e.g. status or department).
     */
    public void filterWith(java.util.function.Predicate<T> predicate) {
        filteredItems.setPredicate(predicate == null ? item -> true : predicate);
        refreshPagination();
    }

    /**
     * Returns true if {@code item} matches {@code query} according to this
     * table's {@link #matches} implementation. Exposed so page controllers
     * can compose compound predicates via {@link #filterWith}.
     */
    public boolean matchesQuery(T item, String query) {
        String lower = query == null ? "" : query.trim().toLowerCase();
        return lower.isEmpty() || matches(item, lower);
    }

    /**
     * Programmatically sort the table by {@code col} in the given direction.
     * Equivalent to the user clicking the column header — updates sort indicators.
     *
     * @param col the column to sort by
     * @param dir {@link TableColumn.SortType#ASCENDING} or DESCENDING
     */
    public void sortByColumn(TableColumn<T, ?> col, TableColumn.SortType dir) {
        col.setSortType(dir);
        table.getSortOrder().setAll(col);
    }

    /**
     * Sorts the table by {@code col} ascending, visually grouping rows that share
     * the same value for that column together.
     */
    public void groupBy(TableColumn<T, ?> col) {
        sortByColumn(col, TableColumn.SortType.ASCENDING);
    }

    /** Clears the active sort and restores natural (insertion) order. */
    public void clearSort() {
        table.getSortOrder().clear();
    }

    public T getSelectedItem() {
        return table.getSelectionModel().getSelectedItem();
    }

    public TableView<T> getTable() {
        return table;
    }

    /** Registers the row-level edit/delete callbacks used by {@link #wireActionsColumn}, with no view-details action. */
    public void setRowActions(Consumer<T> onEdit, Consumer<T> onDelete) {
        setRowActions(onEdit, onDelete, null);
    }

    /** Registers the row-level view/edit/delete callbacks used by {@link #wireActionsColumn}. */
    public void setRowActions(Consumer<T> onEdit, Consumer<T> onDelete, Consumer<T> onViewDetails) {
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.onViewDetails = onViewDetails;
    }

    /**
     * Renders a compact view/edit/delete button trio in the given column, driven by
     * whatever callbacks were registered via {@link #setRowActions}. Every
     * entity table declares one "Actions" TableColumn and wires it with this
     * from its {@link #configureColumns()} instead of hand-rolling cell factories.
     */
    protected void wireActionsColumn(TableColumn<T, Void> actionsColumn) {
        this.actionsColumn = actionsColumn;
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("", new FontIcon("fas-eye"));
            private final Button editBtn = new Button("", new FontIcon("fas-edit"));
            private final Button deleteBtn = new Button("", new FontIcon("fas-trash"));
            private final HBox box = new HBox(4, viewBtn, editBtn, deleteBtn);
            {
                viewBtn.getStyleClass().add("row-action-btn");
                editBtn.getStyleClass().add("row-action-btn");
                deleteBtn.getStyleClass().addAll("row-action-btn", "danger");
                Tooltip.install(viewBtn, new Tooltip("View"));
                Tooltip.install(editBtn, new Tooltip("Edit"));
                Tooltip.install(deleteBtn, new Tooltip("Delete"));
                viewBtn.setOnAction(e -> {
                    if (onViewDetails != null) onViewDetails.accept(getTableView().getItems().get(getIndex()));
                });
                editBtn.setOnAction(e -> {
                    if (onEdit != null) onEdit.accept(getTableView().getItems().get(getIndex()));
                });
                deleteBtn.setOnAction(e -> {
                    if (onDelete != null) onDelete.accept(getTableView().getItems().get(getIndex()));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                viewBtn.setVisible(onViewDetails != null);
                viewBtn.setManaged(onViewDetails != null);
                editBtn.setVisible(onEdit != null);
                editBtn.setManaged(onEdit != null);
                deleteBtn.setVisible(onDelete != null);
                deleteBtn.setManaged(onDelete != null);
                if (onViewDetails == null && onEdit == null && onDelete == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }

    /**
     * Hides the Actions column for read-only usages of this table (e.g. drill-down
     * tabs that never call {@link #setRowActions}) so the edit/delete icons don't
     * render as silent no-ops.
     */
    public void hideActionsColumn() {
        if (actionsColumn != null) {
            actionsColumn.setVisible(false);
        }
    }

    /**
     * Renders a single icon button in the given column, driven by {@code onClick}.
     * Not every table needs this (unlike the universal edit/delete actions column),
     * so subclasses opt in explicitly — e.g. a "change status" action on the handful
     * of tables whose entity has a lifecycle status.
     */
    protected void wireSingleActionColumn(TableColumn<T, Void> column, String iconLiteral, Consumer<T> onClick) {
        wireSingleActionColumn(column, iconLiteral, "Change status", onClick);
    }

    /**
     * Same as {@link #wireSingleActionColumn(TableColumn, String, Consumer)} but with an
     * explicit tooltip, so the action's purpose is self-evident rather than relying on the
     * icon alone (e.g. the "change status" flag icon is otherwise easy to mistake for a
     * billing indicator).
     */
    protected void wireSingleActionColumn(TableColumn<T, Void> column, String iconLiteral, String tooltipText, Consumer<T> onClick) {
        column.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("", new FontIcon(iconLiteral));
            {
                btn.getStyleClass().add("row-action-btn");
                Tooltip.install(btn, new Tooltip(tooltipText));
                btn.setOnAction(e -> {
                    if (onClick != null) onClick.accept(getTableView().getItems().get(getIndex()));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    /**
     * Renders a text-labeled action button (instead of an icon) in the given column,
     * shown only on rows where {@code visible} evaluates to true. Used e.g. for the
     * billing "Paid" action, where a plain word reads better than an icon.
     */
    protected void wireTextActionColumn(TableColumn<T, Void> column, String label, String tooltipText,
                                        Predicate<T> visible, Consumer<T> onClick) {
        column.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button(label);
            {
                btn.getStyleClass().add("row-action-btn");
                Tooltip.install(btn, new Tooltip(tooltipText));
                btn.setOnAction(e -> {
                    if (onClick != null) onClick.accept(getTableView().getItems().get(getIndex()));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                T row = empty ? null : getTableView().getItems().get(getIndex());
                boolean show = !empty && row != null && (visible == null || visible.test(row));
                btn.setVisible(show);
                btn.setManaged(show);
                setGraphic(show ? btn : null);
            }
        });
    }

    /** A label lookup that may fail (e.g. a DB-backed name resolution). */
    @FunctionalInterface
    public interface LabelResolver {
        String resolve() throws Exception;
    }

    /** Resolves a display label for a table cell, falling back to "—" on any lookup failure. */
    protected static String resolveLabel(LabelResolver resolver) {
        try {
            return resolver.resolve();
        } catch (Exception ex) {
            return "—";
        }
    }

    private void refreshPagination() {
        int pageCount = Math.max(1, (int) Math.ceil(filteredItems.size() / (double) ROWS_PER_PAGE));
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        renderPage(0);
    }

    private void renderPage(int pageIndex) {
        List<T> view = new ArrayList<>(filteredItems);
        Comparator<T> comparator = table.getComparator();
        if (comparator != null) {
            AlgorithmUtils.mergeSort(view, comparator);
        }
        int total = view.size();
        int from  = Math.min(pageIndex * ROWS_PER_PAGE, total);
        int to    = Math.min(from + ROWS_PER_PAGE, total);
        // Snapshot the merge-sorted + filtered slice so the table's own sort
        // policy does not try to re-sort the slice in place.
        table.setItems(FXCollections.observableArrayList(view.subList(from, to)));
    }
}
