package hospital.management.backend.utils.pagination;

/**
 * Input to any paginated query.
 * cursor == null means "start from the beginning."
 * cursor != null means "give me the page after this cursor."
 *
 * Build via the static factories on CursorPagination:
 *   PageRequest req = CursorPagination.firstPage();
 *   PageRequest next = CursorPagination.nextPage(result.getNextCursor());
 */
public final class PageRequest {

    public enum SortDirection { ASC, DESC }

    private final String        cursor;
    private final int           pageSize;
    private final SortDirection direction;

    PageRequest(String cursor, int pageSize, SortDirection direction) {
        this.cursor    = cursor;
        this.pageSize  = pageSize;
        this.direction = direction;
    }

    public String        getCursor()    { return cursor; }
    public int           getPageSize()  { return pageSize; }
    public SortDirection getDirection() { return direction; }
    public boolean       isFirstPage()  { return cursor == null; }
}