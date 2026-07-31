package hospital.management.backend.utils.pagination;

import java.util.Collections;
import java.util.List;

/**
 * Output of any paginated query.
 * nextCursor == null means there are no more pages.
 *
 * Usage in a controller:
 *   PageResult<Patient> page = patientService.getAll(CursorPagination.firstPage());
 *   table.setItems(FXCollections.observableArrayList(page.getItems()));
 *   nextBtn.setDisable(!page.hasMore());
 */
public final class PageResult<T> {

    private final List<T> items;
    private final String  nextCursor;
    private final boolean hasMore;
    private final int     pageSize;

    PageResult(List<T> items, String nextCursor, boolean hasMore, int pageSize) {
        this.items      = Collections.unmodifiableList(items);
        this.nextCursor = nextCursor;
        this.hasMore    = hasMore;
        this.pageSize   = pageSize;
    }

    public List<T> getItems()      { return items; }
    public String  getNextCursor() { return nextCursor; }
    public boolean hasMore()       { return hasMore; }
    public int     getPageSize()   { return pageSize; }
    public int     getCount()      { return items.size(); }
    public boolean isEmpty()       { return items.isEmpty(); }
}