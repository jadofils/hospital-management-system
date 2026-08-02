package hospital.management.backend.utils.pagination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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

    /**
     * Converts a page of entities into a page of some other type (typically a DTO),
     * keeping the same cursor/hasMore/pageSize metadata — this is how a service
     * layer turns a DAO's {@code PageResult<Entity>} into a {@code PageResult<Dto>}
     * without needing package-private constructor access.
     */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> mapped = new ArrayList<>(items.size());
        for (T item : items) mapped.add(mapper.apply(item));
        return new PageResult<>(mapped, nextCursor, hasMore, pageSize);
    }
}