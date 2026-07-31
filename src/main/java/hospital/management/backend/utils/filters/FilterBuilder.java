package hospital.management.backend.utils.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fluent builder for composing multiple EntityFilters and applying them to a list.
 *
 * Filters added with and() must ALL pass (intersection).
 * Filters added with or() need only ONE to pass (union).
 *
 * Usage — in-memory filtering of a JavaFX table:
 *
 *   List<Patient> result = FilterBuilder.<Patient>all()
 *       .and(p -> !p.isDeleted())
 *       .and(p -> gender == null || gender.equals(p.getGender()))
 *       .and(p -> search == null
 *              || p.getFullName().toLowerCase().contains(search.toLowerCase()))
 *       .apply(allPatients);
 *
 *   tableItems.setAll(result);
 */
public final class FilterBuilder<T> {

    private EntityFilter<T> combined;

    private FilterBuilder(EntityFilter<T> initial) {
        this.combined = initial;
    }

    /** Starts a builder that passes everything — add conditions with and(). */
    public static <T> FilterBuilder<T> all() {
        return new FilterBuilder<>(item -> true);
    }

    /** Starts a builder that passes nothing — add conditions with or(). */
    public static <T> FilterBuilder<T> none() {
        return new FilterBuilder<>(item -> false);
    }

    // ── Composition ───────────────────────────────────────────────────────────

    /** Adds a condition that must also pass (AND). Null-safe: null filter is ignored. */
    public FilterBuilder<T> and(EntityFilter<T> filter) {
        if (filter != null) combined = combined.and(filter);
        return this;
    }

    /** Adds an alternative condition (OR). Null-safe: null filter is ignored. */
    public FilterBuilder<T> or(EntityFilter<T> filter) {
        if (filter != null) combined = combined.or(filter);
        return this;
    }

    /**
     * Adds a condition only when the guard value is non-null and non-blank.
     * Eliminates the null-check boilerplate at every filter call site.
     *
     * Example:
     *   .andIfPresent(searchText, text -> p -> p.getFullName().contains(text))
     */
    public FilterBuilder<T> andIfPresent(String value,
                                          java.util.function.Function<String, EntityFilter<T>> factory) {
        if (value != null && !value.isBlank()) {
            combined = combined.and(factory.apply(value.strip()));
        }
        return this;
    }

    // ── Apply ─────────────────────────────────────────────────────────────────

    /** Applies the composed filter to a list and returns the matching items. */
    public List<T> apply(List<T> items) {
        if (items == null || items.isEmpty()) return new ArrayList<>();
        return items.stream().filter(combined::test).collect(Collectors.toList());
    }

    /** Returns the composed filter as a single EntityFilter for use in streams. */
    public EntityFilter<T> build() {
        return combined;
    }
}