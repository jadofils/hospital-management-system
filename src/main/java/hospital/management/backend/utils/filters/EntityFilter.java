package hospital.management.backend.utils.filters;

/**
 * A single filter condition on an entity.
 * Returns true to KEEP the item, false to DISCARD it.
 *
 * Functionally identical to java.util.function.Predicate but named
 * for domain clarity and composable via FilterBuilder.
 *
 * Example:
 *   EntityFilter<Patient> activeOnly = p -> !p.isDeleted();
 *   EntityFilter<Patient> femaleOnly = p -> "F".equals(p.getGender());
 */
@FunctionalInterface
public interface EntityFilter<T> {
    boolean test(T item);

    /** Combines this filter AND another — both must pass. */
    default EntityFilter<T> and(EntityFilter<T> other) {
        return item -> this.test(item) && other.test(item);
    }

    /** Combines this filter OR another — at least one must pass. */
    default EntityFilter<T> or(EntityFilter<T> other) {
        return item -> this.test(item) || other.test(item);
    }

    /** Inverts this filter. */
    default EntityFilter<T> negate() {
        return item -> !this.test(item);
    }
}