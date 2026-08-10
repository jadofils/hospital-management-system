package hospital.management.pages.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleStringProperty;
import java.lang.reflect.Method;
import java.util.Comparator;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.function.Consumer;

/**
 * Shared base for every entity table (patients, doctors, appointments, ...).
 * Every *-table.fxml declares its fx:controller as a subclass of this, with
 * fx:id="table" and fx:id="pagination" on the TableView/Pagination.
 *
 * Subclasses only implement column binding and the search predicate; the
 * paging/filtering mechanics live here once instead of per-entity.
 */
public abstract class PaginatedTableController<T> {

    protected static final int ROWS_PER_PAGE = 10;

    @FXML protected TableView<T> table;
    @FXML protected Pagination pagination;

    protected final ObservableList<T> sourceItems = FXCollections.observableArrayList();
    protected FilteredList<T> filteredItems;
    protected SortedList<T>   sortedItems;

    private Consumer<T> onEdit;
    private Consumer<T> onDelete;
    private Consumer<T> onViewDetails;
    private TableColumn<T, Void> actionsColumn;

    public void initialize() {
        configureColumns();
        ensureIdColumn();
        filteredItems = new FilteredList<>(sourceItems, item -> true);
        sortedItems   = new SortedList<>(filteredItems);
        // Bind sortedItems to the table's active comparator so clicking a column
        // header re-sorts the *full* filtered dataset before we slice a page.
        sortedItems.comparatorProperty().bind(table.comparatorProperty());
        // Prevent TableView from sorting its displayed page-slice in-place;
        // our SortedList + renderPage listener already handles the correct ordering.
        table.setSortPolicy(t -> true);
        // Re-render the current page whenever the sort order changes.
        table.comparatorProperty().addListener((obs, o, n) -> renderPage(pagination.getCurrentPageIndex()));
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        pagination.currentPageIndexProperty().addListener((obs, o, n) -> renderPage(n.intValue()));
        refreshPagination();
    }

    /**
     * Adds an `ID` column at the front of the table when possible. The column
     * attempts to extract a readable id by reflection (getId() or any getter
     * ending with "Id"). Falls back to object's hashCode when no id accessor
     * is found. This is a frontend-only display aid and does not modify data.
     */
    @SuppressWarnings("unchecked")
    private void ensureIdColumn() {
        // If a column named "ID" already exists, do nothing.
        boolean hasId = table.getColumns().stream().anyMatch(c -> "ID".equalsIgnoreCase(c.getText()));
        if (hasId) return;

        TableColumn<T, String> idCol = new TableColumn<>("ID");
        idCol.setMinWidth(120);
        idCol.setCellValueFactory(cellData -> {
            T item = cellData.getValue();
            if (item == null) return new SimpleStringProperty("");
            String id = extractId(item);
            return new SimpleStringProperty(id == null ? "" : id);
        });

        // Insert ID column at the left-most position so it's visible in paginated lists
        table.getColumns().add(0, idCol);
    }

    private String extractId(T item) {
        try {
            Class<?> cls = item.getClass();
            // Prefer explicit getId()
            try {
                Method m = cls.getMethod("getId");
                Object val = m.invoke(item);
                if (val != null) return String.valueOf(val);
            } catch (NoSuchMethodException ignored) {
            }
            // Fallback: any getter that ends with 'Id'
            for (Method m : cls.getMethods()) {
                String name = m.getName();
                if (name.startsWith("get") && name.length() > 3 && name.toLowerCase().endsWith("id") && m.getParameterCount() == 0) {
                    Object val = m.invoke(item);
                    if (val != null) return String.valueOf(val);
                }
            }
        } catch (Exception ignored) {
        }
        return String.valueOf(item.hashCode());
    }

    /** Bind columns to the entity's properties. Called once during {@link #initialize()}. */
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
        column.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("", new FontIcon(iconLiteral));
            {
                btn.getStyleClass().add("row-action-btn");
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

    private void refreshPagination() {
        int pageCount = Math.max(1, (int) Math.ceil(sortedItems.size() / (double) ROWS_PER_PAGE));
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        renderPage(0);
    }

    private void renderPage(int pageIndex) {
        int total = sortedItems.size();
        int from  = Math.min(pageIndex * ROWS_PER_PAGE, total);
        int to    = Math.min(from + ROWS_PER_PAGE, total);
        // Snapshot the sorted+filtered slice so the table's own sort policy
        // does not try to re-sort the slice in place (handled by SortedList).
        table.setItems(FXCollections.observableArrayList(sortedItems.subList(from, to)));
    }
}
