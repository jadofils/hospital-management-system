package hospital.management.backend.utils.filters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlFilterBuilderTest {

    @Test
    @DisplayName("A fresh builder with no conditions produces an empty WHERE clause and no params")
    void emptyBuilder_producesNoClause() {
        SqlFilterBuilder filter = SqlFilterBuilder.start();
        assertTrue(filter.isEmpty());
        assertEquals("", filter.buildWhere());
        assertTrue(filter.getParams().isEmpty());
    }

    @Test
    @DisplayName("andRaw appends a fixed fragment with no parameter binding")
    void andRaw_appendsFragmentNoParam() {
        SqlFilterBuilder filter = SqlFilterBuilder.start().andRaw("deleted_at IS NULL");
        assertEquals("WHERE deleted_at IS NULL ", filter.buildWhere());
        assertTrue(filter.getParams().isEmpty());
    }

    @Test
    @DisplayName("andEquals adds a bound condition when the value is non-null")
    void andEquals_addsConditionWhenNonNull() {
        SqlFilterBuilder filter = SqlFilterBuilder.start().andEquals("gender", "Female");
        assertEquals("WHERE gender = ? ", filter.buildWhere());
        assertEquals(List.of("Female"), filter.getParams());
    }

    @Test
    @DisplayName("andEquals is silently skipped when the value is null")
    void andEquals_skippedWhenNull() {
        SqlFilterBuilder filter = SqlFilterBuilder.start().andEquals("gender", null);
        assertTrue(filter.isEmpty());
        assertEquals("", filter.buildWhere());
    }

    @Test
    @DisplayName("andNotEquals adds a != condition when non-null, skipped when null")
    void andNotEquals_behavesLikeEquals() {
        SqlFilterBuilder filter = SqlFilterBuilder.start().andNotEquals("status", "Cancelled");
        assertEquals("WHERE status != ? ", filter.buildWhere());
        assertEquals(List.of("Cancelled"), filter.getParams());

        assertTrue(SqlFilterBuilder.start().andNotEquals("status", null).isEmpty());
    }

    @Test
    @DisplayName("andLike wraps the value in wildcards and trims it, skipped when blank")
    void andLike_wrapsInWildcardsAndTrims() {
        SqlFilterBuilder filter = SqlFilterBuilder.start().andLike("first_name", "  Jane  ");
        assertEquals("WHERE first_name ILIKE ? ", filter.buildWhere());
        assertEquals(List.of("%Jane%"), filter.getParams());
    }

    @Test
    @DisplayName("andLike is skipped for null or blank search values")
    void andLike_skippedWhenBlank() {
        assertTrue(SqlFilterBuilder.start().andLike("first_name", null).isEmpty());
        assertTrue(SqlFilterBuilder.start().andLike("first_name", "   ").isEmpty());
    }

    @Test
    @DisplayName("andFrom/andTo add inclusive range bounds when non-null")
    void andFromAndTo_addRangeBounds() {
        SqlFilterBuilder filter = SqlFilterBuilder.start()
                .andFrom("created_at", "2026-01-01")
                .andTo("created_at", "2026-12-31");
        assertEquals("WHERE created_at >= ? AND created_at <= ? ", filter.buildWhere());
        assertEquals(List.of("2026-01-01", "2026-12-31"), filter.getParams());
    }

    @Test
    @DisplayName("andIsNull / andIsNotNull always append regardless of any value")
    void andIsNullAndIsNotNull_alwaysAppend() {
        SqlFilterBuilder filter = SqlFilterBuilder.start()
                .andIsNull("deleted_at")
                .andIsNotNull("email");
        assertEquals("WHERE deleted_at IS NULL AND email IS NOT NULL ", filter.buildWhere());
        assertTrue(filter.getParams().isEmpty());
    }

    @Test
    @DisplayName("Multiple conditions are joined with AND in the order they were added")
    void multipleConditions_joinedInOrder() {
        SqlFilterBuilder filter = SqlFilterBuilder.start()
                .andRaw("deleted_at IS NULL")
                .andEquals("gender", "Male")
                .andLike("last_name", "Smith");
        assertEquals("WHERE deleted_at IS NULL AND gender = ? AND last_name ILIKE ? ", filter.buildWhere());
        assertEquals(List.of("Male", "%Smith%"), filter.getParams());
    }

    @Test
    @DisplayName("getParams returns a mutable copy independent of the builder's internal state")
    void getParams_returnsIndependentMutableCopy() {
        SqlFilterBuilder filter = SqlFilterBuilder.start().andEquals("status", "Active");
        List<Object> params = filter.getParams();
        params.add("extra"); // mutate the returned copy
        assertEquals(1, filter.getParams().size(), "builder's own params must be unaffected by mutating a previously-returned copy");
    }
}
