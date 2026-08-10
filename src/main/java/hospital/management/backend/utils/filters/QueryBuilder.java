package hospital.management.backend.utils.filters;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent, single-use SQL SELECT query builder.
 *
 * <p>Typical usage:
 * <pre>{@code
 * String sql = QueryBuilder.select("p.patient_id", "p.first_name", "p.last_name")
 *     .from("patients p")
 *     .leftJoin("appointments a ON a.patient_id = p.patient_id")
 *     .whereActive("p")
 *     .and("p.first_name ILIKE ?")
 *     .orderBy("p.last_name")
 *     .limit(20)
 *     .offset(0)
 *     .build();
 * }</pre>
 *
 * <p>Not thread-safe — build one instance per query, discard after {@link #build()}.
 */
public class QueryBuilder {

    /** Sort direction for {@link #orderBy(String, SortDir)}. */
    public enum SortDir { ASC, DESC }

    // ── Internal state ────────────────────────────────────────────────────

    private final List<String> selectCols;
    private boolean distinct = false;
    private String  fromExpr;
    private final List<String> joins        = new ArrayList<>();
    private final List<String> conditions   = new ArrayList<>();
    private final List<String> groupByParts = new ArrayList<>();
    private String havingClause;
    private final List<String> orderParts   = new ArrayList<>();
    private int limitVal  = -1;
    private int offsetVal = -1;

    // ── Constructors ──────────────────────────────────────────────────────

    private QueryBuilder(List<String> columns) {
        this.selectCols = columns;
    }

    // ── Static factory methods ────────────────────────────────────────────

    /**
     * Starts a SELECT query with the given columns.
     * Each entry may be a simple name ({@code "first_name"}) or an expression
     * with alias ({@code "COUNT(*) AS total"}).
     */
    public static QueryBuilder select(String... columns) {
        List<String> cols = new ArrayList<>();
        for (String c : columns) cols.add(c);
        return new QueryBuilder(cols);
    }

    /** Starts a {@code SELECT *} query. */
    public static QueryBuilder selectAll() {
        List<String> cols = new ArrayList<>();
        cols.add("*");
        return new QueryBuilder(cols);
    }

    // ── Modifier methods ──────────────────────────────────────────────────

    /** Adds the {@code DISTINCT} keyword after {@code SELECT}. */
    public QueryBuilder distinct() {
        this.distinct = true;
        return this;
    }

    /**
     * Sets the FROM clause. May include an alias:
     * {@code .from("patients p")} produces {@code FROM patients p}.
     */
    public QueryBuilder from(String tableExpr) {
        this.fromExpr = tableExpr;
        return this;
    }

    /** Appends an {@code INNER JOIN} clause. */
    public QueryBuilder join(String joinExpr) {
        joins.add("INNER JOIN " + joinExpr);
        return this;
    }

    /** Appends a {@code LEFT JOIN} clause. */
    public QueryBuilder leftJoin(String joinExpr) {
        joins.add("LEFT JOIN " + joinExpr);
        return this;
    }

    /**
     * Starts the WHERE clause with the given condition.
     * Calling this more than once replaces the first condition; use
     * {@link #and(String)} for additional conditions.
     */
    public QueryBuilder where(String condition) {
        conditions.add(condition);
        return this;
    }

    /** Appends an {@code AND} condition to the WHERE clause. */
    public QueryBuilder and(String condition) {
        conditions.add(condition);
        return this;
    }

    /**
     * Adds {@code deleted_at IS NULL} to the WHERE clause.
     * Assumes no table alias prefix; use {@link #whereActive(String)} when
     * a prefix is needed.
     */
    public QueryBuilder whereActive() {
        conditions.add("deleted_at IS NULL");
        return this;
    }

    /**
     * Adds {@code alias.deleted_at IS NULL} to the WHERE clause.
     *
     * @param tableAlias the table alias prefix, e.g. {@code "p"}
     */
    public QueryBuilder whereActive(String tableAlias) {
        conditions.add(tableAlias + ".deleted_at IS NULL");
        return this;
    }

    /**
     * Adds a case-insensitive LIKE condition: {@code column ILIKE ?}.
     *
     * @param column           the column (or expression) to match
     * @param paramPlaceholder the bind-parameter placeholder, typically {@code "?"}
     */
    public QueryBuilder whereLike(String column, String paramPlaceholder) {
        conditions.add(column + " ILIKE " + paramPlaceholder);
        return this;
    }

    /**
     * Adds a multi-column full-text search condition:
     * {@code (col1 ILIKE ? OR col2 ILIKE ? OR ...)}.
     *
     * <p>The same {@code paramPlaceholder} is repeated for every column, so the
     * caller must bind the same search value N times (once per column) in the
     * prepared statement.
     *
     * @param paramPlaceholder bind placeholder to repeat, typically {@code "?"}
     * @param columns          one or more columns to search
     */
    public QueryBuilder whereSearchAny(String paramPlaceholder, String... columns) {
        if (columns.length == 0) return this;
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(" OR ");
            sb.append(columns[i]).append(" ILIKE ").append(paramPlaceholder);
        }
        sb.append(")");
        conditions.add(sb.toString());
        return this;
    }

    /** Adds a GROUP BY clause. */
    public QueryBuilder groupBy(String... columns) {
        for (String c : columns) groupByParts.add(c);
        return this;
    }

    /** Adds a HAVING clause. Only meaningful if {@link #groupBy} was also called. */
    public QueryBuilder having(String condition) {
        this.havingClause = condition;
        return this;
    }

    /**
     * Adds an ORDER BY column in ascending order.
     * Equivalent to {@link #orderBy(String, SortDir) orderBy(column, SortDir.ASC)}.
     */
    public QueryBuilder orderBy(String column) {
        orderParts.add(column + " ASC");
        return this;
    }

    /**
     * Adds an ORDER BY column with explicit direction.
     *
     * @param column the column (or expression) to sort by
     * @param dir    {@link SortDir#ASC} or {@link SortDir#DESC}
     */
    public QueryBuilder orderBy(String column, SortDir dir) {
        orderParts.add(column + " " + dir.name());
        return this;
    }

    /** Adds a LIMIT clause. Pass a positive integer. */
    public QueryBuilder limit(int n) {
        this.limitVal = n;
        return this;
    }

    /** Adds an OFFSET clause. Pass a non-negative integer. */
    public QueryBuilder offset(int n) {
        this.offsetVal = n;
        return this;
    }

    // ── Build ─────────────────────────────────────────────────────────────

    /**
     * Builds and returns the final SQL string.
     *
     * @throws IllegalStateException if {@link #from(String)} was never called
     */
    public String build() {
        if (fromExpr == null || fromExpr.isBlank()) {
            throw new IllegalStateException(
                "QueryBuilder: FROM clause is required but was not set. Call .from(\"...\") before .build().");
        }

        StringBuilder sb = new StringBuilder();

        // SELECT [DISTINCT] col1, col2, ...
        sb.append("SELECT ");
        if (distinct) sb.append("DISTINCT ");
        sb.append(String.join(", ", selectCols));

        // FROM
        sb.append(" FROM ").append(fromExpr);

        // JOIN(s)
        for (String j : joins) {
            sb.append(" ").append(j);
        }

        // WHERE
        if (!conditions.isEmpty()) {
            sb.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        // GROUP BY
        if (!groupByParts.isEmpty()) {
            sb.append(" GROUP BY ").append(String.join(", ", groupByParts));
        }

        // HAVING
        if (havingClause != null && !havingClause.isBlank()) {
            sb.append(" HAVING ").append(havingClause);
        }

        // ORDER BY
        if (!orderParts.isEmpty()) {
            sb.append(" ORDER BY ").append(String.join(", ", orderParts));
        }

        // LIMIT / OFFSET
        if (limitVal >= 0) {
            sb.append(" LIMIT ").append(limitVal);
        }
        if (offsetVal >= 0) {
            sb.append(" OFFSET ").append(offsetVal);
        }

        return sb.toString();
    }

    /**
     * Delegates to {@link #build()}.
     *
     * @throws IllegalStateException if the FROM clause was not set
     */
    @Override
    public String toString() {
        return build();
    }
}