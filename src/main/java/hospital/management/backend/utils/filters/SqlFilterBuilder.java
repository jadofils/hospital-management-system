package hospital.management.backend.utils.filters;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a parameterized SQL WHERE clause from named conditions.
 * Works alongside CursorPagination — add the cursor WHERE fragment first,
 * then layer domain filters on top.
 *
 * All values are collected as positional parameters (?), never interpolated
 * into the SQL string, preventing SQL injection.
 *
 * Usage in a DAO:
 *
 *   SqlFilterBuilder filter = SqlFilterBuilder.start()
 *       .andRaw("deleted_at IS NULL")
 *       .andEquals("gender", gender)            // skipped if gender is null
 *       .andLike("first_name", search)          // skipped if search is null/blank
 *       .andEquals("status", status);
 *
 *   String sql = "SELECT * FROM patients " + filter.buildWhere()
 *              + CursorPagination.orderClause(req, "created_at")
 *              + " LIMIT ?";
 *
 *   // Bind: filter.getParams() first, then pageSize+1
 *   List<Object> params = filter.getParams();
 *   params.add(req.getPageSize() + 1);
 */
public final class SqlFilterBuilder {

    private final List<String> clauses = new ArrayList<>();
    private final List<Object> params  = new ArrayList<>();

    private SqlFilterBuilder() {}

    /** Starts a new builder with no conditions. */
    public static SqlFilterBuilder start() {
        return new SqlFilterBuilder();
    }

    // ── Conditions ────────────────────────────────────────────────────────────

    /**
     * Appends a raw SQL fragment with no parameter binding.
     * Use for fixed conditions like "deleted_at IS NULL".
     */
    public SqlFilterBuilder andRaw(String sql) {
        clauses.add(sql);
        return this;
    }

    /**
     * Adds column = ? if value is non-null.
     * Skipped silently when value is null — no WHERE fragment is added.
     */
    public SqlFilterBuilder andEquals(String column, Object value) {
        if (value != null) {
            clauses.add(column + " = ?");
            params.add(value);
        }
        return this;
    }

    /**
     * Adds column != ? if value is non-null.
     */
    public SqlFilterBuilder andNotEquals(String column, Object value) {
        if (value != null) {
            clauses.add(column + " != ?");
            params.add(value);
        }
        return this;
    }

    /**
     * Adds ILIKE (case-insensitive LIKE) if value is non-null and non-blank.
     * Automatically wraps the value in % wildcards.
     * PostgreSQL ILIKE is used — adjust to LOWER(col) LIKE LOWER(?) for other DBs.
     */
    public SqlFilterBuilder andLike(String column, String value) {
        if (value != null && !value.isBlank()) {
            clauses.add(column + " ILIKE ?");
            params.add("%" + value.strip() + "%");
        }
        return this;
    }

    /**
     * Adds column >= ? if value is non-null (range start, inclusive).
     */
    public SqlFilterBuilder andFrom(String column, Object value) {
        if (value != null) {
            clauses.add(column + " >= ?");
            params.add(value);
        }
        return this;
    }

    /**
     * Adds column <= ? if value is non-null (range end, inclusive).
     */
    public SqlFilterBuilder andTo(String column, Object value) {
        if (value != null) {
            clauses.add(column + " <= ?");
            params.add(value);
        }
        return this;
    }

    /**
     * Adds column IS NULL.
     */
    public SqlFilterBuilder andIsNull(String column) {
        clauses.add(column + " IS NULL");
        return this;
    }

    /**
     * Adds column IS NOT NULL.
     */
    public SqlFilterBuilder andIsNotNull(String column) {
        clauses.add(column + " IS NOT NULL");
        return this;
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    /**
     * Returns the full WHERE clause string, or an empty string if no conditions
     * were added. Never returns "WHERE" alone.
     *
     * Example output: "WHERE deleted_at IS NULL AND gender = ? AND first_name ILIKE ?"
     */
    public String buildWhere() {
        if (clauses.isEmpty()) return "";
        return "WHERE " + String.join(" AND ", clauses) + " ";
    }

    /**
     * Returns the bound parameter values in the same order as the ? placeholders.
     * Pass these to PreparedStatement.setObject(index, param) in sequence.
     *
     * Returns a mutable copy so callers can append pagination params.
     */
    public List<Object> getParams() {
        return new ArrayList<>(params);
    }

    /** Returns true if no conditions have been added yet. */
    public boolean isEmpty() {
        return clauses.isEmpty();
    }
}