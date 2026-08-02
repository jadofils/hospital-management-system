package hospital.management.backend.utils.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CursorPaginationTest {

    private static final LocalDateTime TS = LocalDateTime.of(2026, 8, 1, 10, 30, 0);

    // ── Page request factories ───────────────────────────────────────────

    @Test
    @DisplayName("firstPage(size) has no cursor, the given size, and DESC direction")
    void firstPageWithSize_hasNoCursor() {
        PageRequest req = CursorPagination.firstPage(50);
        assertTrue(req.isFirstPage());
        assertNull(req.getCursor());
        assertEquals(50, req.getPageSize());
        assertEquals(PageRequest.SortDirection.DESC, req.getDirection());
    }

    @Test
    @DisplayName("nextPage(cursor, size) carries the given cursor and is not a first page")
    void nextPageWithSize_carriesCursor() {
        String cursor = CursorPagination.encodeCursor(TS);
        PageRequest req = CursorPagination.nextPage(cursor, 20);
        assertFalse(req.isFirstPage());
        assertEquals(cursor, req.getCursor());
        assertEquals(20, req.getPageSize());
    }

    // ── Cursor encode/decode round-trip ───────────────────────────────────

    @Test
    @DisplayName("encodeCursor then decodeCursor round-trips to the exact same timestamp")
    void encodeThenDecode_roundTrips() {
        String encoded = CursorPagination.encodeCursor(TS);
        assertEquals(TS, CursorPagination.decodeCursor(encoded));
    }

    @Test
    @DisplayName("An encoded cursor is URL-safe Base64 (no '+', '/', or padding '=')")
    void encodedCursor_isUrlSafeBase64() {
        String encoded = CursorPagination.encodeCursor(TS);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        assertFalse(encoded.contains("="));
    }

    // ── whereClause ───────────────────────────────────────────────────────

    @Test
    @DisplayName("whereClause is empty on the first page (no cursor constraint needed)")
    void whereClause_emptyOnFirstPage() {
        PageRequest req = CursorPagination.firstPage(20);
        assertEquals("", CursorPagination.whereClause(req, "created_at"));
    }

    @Test
    @DisplayName("whereClause uses '<' for DESC direction, anchored on the decoded cursor timestamp")
    void whereClause_usesLessThanForDesc() {
        String cursor = CursorPagination.encodeCursor(TS);
        PageRequest req = CursorPagination.nextPage(cursor, 20);
        String clause = CursorPagination.whereClause(req, "created_at");
        assertTrue(clause.startsWith("AND created_at < '"));
        assertTrue(clause.contains("2026-08-01T10:30:00"));
    }

    // ── orderClause ───────────────────────────────────────────────────────

    @Test
    @DisplayName("orderClause reflects the request's sort direction")
    void orderClause_reflectsDirection() {
        PageRequest req = CursorPagination.firstPage(20);
        assertEquals("ORDER BY created_at DESC ", CursorPagination.orderClause(req, "created_at"));
    }

    // ── toResult ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("toResult with fewer rows than the page size reports hasMore=false and no next cursor")
    void toResult_noMoreRowsThanPageSize() {
        PageRequest req = CursorPagination.firstPage(10);
        List<LocalDateTime> rows = List.of(TS, TS.minusDays(1), TS.minusDays(2));

        PageResult<LocalDateTime> result = CursorPagination.toResult(rows, req, ts -> ts);

        assertEquals(3, result.getCount());
        assertFalse(result.hasMore());
        assertNull(result.getNextCursor());
    }

    @Test
    @DisplayName("toResult with exactly pageSize+1 rows trims the extra row and sets hasMore=true")
    void toResult_extraRowSignalsHasMore() {
        PageRequest req = CursorPagination.firstPage(2);
        // 3 rows fetched for a page size of 2 — the 3rd is the "is there more?" probe row.
        LocalDateTime row1 = TS;
        LocalDateTime row2 = TS.minusDays(1);
        LocalDateTime row3 = TS.minusDays(2);
        List<LocalDateTime> rows = List.of(row1, row2, row3);

        PageResult<LocalDateTime> result = CursorPagination.toResult(rows, req, ts -> ts);

        assertEquals(2, result.getCount());
        assertTrue(result.hasMore());
        assertNotNull(result.getNextCursor());
        assertEquals(row2, CursorPagination.decodeCursor(result.getNextCursor()),
                "next cursor should anchor on the last item actually returned (row2), not the trimmed probe row");
    }

    @Test
    @DisplayName("toResult on an empty row list reports an empty, no-more-pages result")
    void toResult_emptyRows() {
        PageRequest req = CursorPagination.firstPage(10);
        PageResult<LocalDateTime> result = CursorPagination.toResult(List.of(), req, ts -> ts);

        assertTrue(result.isEmpty());
        assertFalse(result.hasMore());
        assertNull(result.getNextCursor());
    }
}
