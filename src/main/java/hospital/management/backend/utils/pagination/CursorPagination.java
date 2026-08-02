package hospital.management.backend.utils.pagination;

import hospital.management.backend.config.AppConfig;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

/**
 * Cursor-based pagination helper for any JDBC service.
 *
 * WHY cursor instead of LIMIT/OFFSET?
 * OFFSET skips rows at query time — if a row is inserted before your offset, you
 * see duplicates on the next page. A cursor anchors to a specific point in the
 * dataset (created_at timestamp) so pages are always stable regardless of writes.
 *
 * Cursor format: Base64URL(ISO-8601 datetime string)
 * The cursor is opaque to callers — they pass it back as-is.
 *
 * DAO usage:
 *   public PageResult<Patient> getAll(PageRequest req) throws DatabaseException {
 *       String sql = "SELECT * FROM patients WHERE deleted_at IS NULL "
 *                  + CursorPagination.whereClause(req, "created_at")
 *                  + CursorPagination.orderClause(req, "created_at")
 *                  + " LIMIT ?";
 *       // bind pageSize + 1 — the extra row tells us if there is a next page
 *       List<Patient> rows = queryWithLimit(sql, req.getPageSize() + 1);
 *       return CursorPagination.toResult(rows, req, p -> p.getCreatedAt().toString());
 *   }
 */
public final class CursorPagination {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private CursorPagination() {}

    // ── Page request factories ────────────────────────────────────────────────

    /** First page, using the default page size from AppConfig. */
    public static PageRequest firstPage() {
        return new PageRequest(null, AppConfig.getPageSize(), PageRequest.SortDirection.DESC);
    }

    /** First page with an explicit size. */
    public static PageRequest firstPage(int size) {
        return new PageRequest(null, size, PageRequest.SortDirection.DESC);
    }

    /** Next page — pass the nextCursor from the previous PageResult. */
    public static PageRequest nextPage(String cursor) {
        return new PageRequest(cursor, AppConfig.getPageSize(), PageRequest.SortDirection.DESC);
    }

    public static PageRequest nextPage(String cursor, int size) {
        return new PageRequest(cursor, size, PageRequest.SortDirection.DESC);
    }

    // ── SQL fragment builders ─────────────────────────────────────────────────

    /**
     * Returns the cursor WHERE clause to append to your base query.
     * Returns an empty string on the first page (no cursor constraint needed).
     *
     * @param req          the PageRequest from the caller
     * @param cursorColumn the DB column used as the cursor anchor, e.g. "created_at"
     */
    public static String whereClause(PageRequest req, String cursorColumn) {
        if (req.isFirstPage()) return "";
        String op = req.getDirection() == PageRequest.SortDirection.DESC ? "<" : ">";
        LocalDateTime ts = decodeCursor(req.getCursor());
        return "AND " + cursorColumn + " " + op + " '" + ts.format(FMT) + "' ";
    }

    /**
     * Returns the ORDER BY + LIMIT clause.
     * Always request pageSize + 1 rows — the extra row is used to detect hasMore.
     *
     * @param cursorColumn the same column used in whereClause
     */
    public static String orderClause(PageRequest req, String cursorColumn) {
        String dir = req.getDirection().name();
        return "ORDER BY " + cursorColumn + " " + dir + " ";
    }

    // ── Result builder ────────────────────────────────────────────────────────

    /**
     * Converts a raw list (fetched with LIMIT pageSize+1) into a PageResult.
     * If the list has more than pageSize rows, the last row is trimmed and
     * nextCursor is set to the cursor of the new last row.
     *
     * @param rows            raw query results, fetched with pageSize+1 limit
     * @param req             the original PageRequest
     * @param cursorExtractor extracts the cursor field value from a row (e.g. row::getCreatedAt)
     */
    public static <T> PageResult<T> toResult(
            List<T> rows, PageRequest req, Function<T, LocalDateTime> cursorExtractor) {

        boolean hasMore = rows.size() > req.getPageSize();
        List<T> items   = hasMore ? rows.subList(0, req.getPageSize()) : rows;

        String nextCursor = null;
        if (hasMore) {
            LocalDateTime lastTs = cursorExtractor.apply(items.get(items.size() - 1));
            nextCursor = encodeCursor(lastTs);
        }
        return new PageResult<>(items, nextCursor, hasMore, req.getPageSize());
    }

    // ── Cursor encoding ───────────────────────────────────────────────────────

    public static String encodeCursor(LocalDateTime ts) {
        return Base64.getUrlEncoder().withoutPadding()
                     .encodeToString(ts.format(FMT).getBytes(StandardCharsets.UTF_8));
    }

    public static LocalDateTime decodeCursor(String cursor) {
        String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        return LocalDateTime.parse(raw, FMT);
    }
}