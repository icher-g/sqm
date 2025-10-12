package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FilterSpecParser:
 *  - single-column operators: IN / NOT IN / LIKE / BETWEEN / = != <> > >= < <=
 *  - tuple IN: (a,b) IN ((1,2),(3,4))
 *  - boolean composition with precedence and grouping: NOT > AND > OR
 *  - escaped quotes in strings
 *  - error handling
 * Adjust assertions if your concrete type names differ.
 */
class FilterParserTest {

    private final FilterParser parser = new FilterParser();

    /* ------------------------ single-column operators ------------------------ */

    @Test
    @DisplayName("IN builds ColumnFilter with Values.ListVals")
    void in_list() {
        var r = parser.parse("category IN (1, 2, 3)");
        assertOk(r);
        var f = assertIs(ColumnFilter.class, r.value());
        assertEquals(ColumnFilter.Operator.In, f.op());
        var v = assertIs(Values.ListValues.class, f.values());
        assertEquals(List.of(1L, 2L, 3L), v.items()); // longs if your parser maps integers to Long
        assertEquals("category", f.columnAs(NamedColumn.class).name());
    }

    @Test
    @DisplayName("NOT IN builds ColumnFilter with Values.ListVals")
    void not_in_list() {
        var r = parser.parse("status NOT IN ('A','B')");
        assertOk(r);
        var f = assertIs(ColumnFilter.class, r.value());
        assertEquals(ColumnFilter.Operator.NotIn, f.op());
        var v = assertIs(Values.ListValues.class, f.values());
        assertEquals(List.of("A", "B"), v.items());
        assertEquals("status", f.columnAs(NamedColumn.class).name());
    }

    @Test
    @DisplayName("LIKE builds ColumnFilter with Values.Single")
    void like_single() {
        var r = parser.parse("name LIKE '%abc%'");
        assertOk(r);
        var f = assertIs(ColumnFilter.class, r.value());
        assertEquals(ColumnFilter.Operator.Like, f.op());
        var v = assertIs(Values.Single.class, f.values());
        assertEquals("%abc%", v.value());
        assertEquals("name", f.columnAs(NamedColumn.class).name());
    }

    @Test
    @DisplayName("BETWEEN builds ColumnFilter with Values.Range")
    void between_range() {
        var r = parser.parse("price BETWEEN 10 AND 20");
        assertOk(r);
        var f = assertIs(ColumnFilter.class, r.value());
        assertEquals(ColumnFilter.Operator.Range, f.op());
        var v = assertIs(Values.Range.class, f.values());
        assertEquals(10L, v.min());
        assertEquals(20L, v.max());
        assertEquals("price", f.columnAs(NamedColumn.class).name());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "qty = 5,Eq,5",
            "qty != 5,Ne,5",
            "qty <> 5,Ne,5",
            "qty > 5,Gt,5",
            "qty >= 5,Gte,5",
            "qty < 5,Lt,5",
            "qty <= 5,Lte,5"
    })
    @DisplayName("Comparison operators map to FilterOperator and Values.Single")
    void comparisons(String expr, ColumnFilter.Operator operator, long expected) {
        var r = parser.parse(expr);
        assertOk(r);
        var f = assertIs(ColumnFilter.class, r.value());
        assertEquals(operator, f.op());
        var v = assertIs(Values.Single.class, f.values());
        assertEquals(expected, v.value());
        assertEquals("qty", f.columnAs(NamedColumn.class).name());
    }

    /* ----------------------------- tuple IN --------------------------------- */

    @Test
    @DisplayName("Tuple IN: (a,b) IN ((1,2),(3,4)) builds TupleColumnFilter + Values.Tuples")
    void tuple_in() {
        var r = parser.parse("(a, b) IN ((1,2), (3,4))");
        assertOk(r);
        var f = assertIs(TupleFilter.class, r.value());
        var c0 = (NamedColumn)f.columns().get(0);
        var c1 = (NamedColumn)f.columns().get(1);
        assertEquals(TupleFilter.Operator.In, f.operator());
        assertEquals(2, f.columns().size());
        assertEquals("a", c0.name());
        assertEquals("b", c1.name());

        var v = assertIs(Values.Tuples.class, f.values());
        assertEquals(List.of(List.of(1L, 2L), List.of(3L, 4L)), v.rows());
    }

    /* --------------------- boolean composition & precedence ------------------ */

    @Test
    @DisplayName("AND has higher precedence than OR when grouped by parser (NOT > AND > OR)")
    void boolean_precedence_and_grouping() {
        var r = parser.parse("status IN ('A','B') AND (price BETWEEN 10 AND 20 OR NOT name LIKE '%test%')");
        assertOk(r);

        var root = assertIs(CompositeFilter.class, r.value());
        assertEquals(CompositeFilter.Operator.And, root.op());
        assertEquals(2, root.filters().size());

        // left is ColumnFilter IN
        assertIs(ColumnFilter.class, root.filters().get(0));

        // right is ( ... OR NOT ... )
        var right = assertIs(CompositeFilter.class, root.filters().get(1));
        assertEquals(CompositeFilter.Operator.Or, right.op());
        assertEquals(2, right.filters().size());

        var not = assertIs(CompositeFilter.class, right.filters().get(1));
        assertEquals(CompositeFilter.Operator.Not, not.op());
        assertEquals(1, not.filters().size());
        assertIs(ColumnFilter.class, not.filters().get(0));
    }

    @Test
    @DisplayName("NOT binds tighter than AND/OR; parentheses preserved")
    void boolean_not_precedence() {
        var r = parser.parse("NOT status IN ('X') OR price >= 100");
        assertOk(r);

        var or = assertIs(CompositeFilter.class, r.value());
        assertEquals(CompositeFilter.Operator.Or, or.op());
        assertEquals(2, or.filters().size());

        var left = assertIs(CompositeFilter.class, or.filters().get(0));
        assertEquals(CompositeFilter.Operator.Not, left.op());
        assertIs(ColumnFilter.class, left.filters().get(0));
    }

    /* ------------------------------ literals -------------------------------- */

    @Test
    @DisplayName("Escaped quote in string literal: 'O''Reilly'")
    void escaped_quotes() {
        var r = parser.parse("name LIKE 'O''Reilly'");
        assertOk(r);
        var f = assertIs(ColumnFilter.class, r.value());
        var v = assertIs(Values.Single.class, f.values());
        assertEquals("O'Reilly", v.value());
    }

    @Test
    @DisplayName("Booleans and NULL literals")
    void boolean_and_null_literals() {
        var r1 = parser.parse("active = TRUE");
        assertOk(r1);
        assertEquals(Boolean.TRUE, ((Values.Single) ((ColumnFilter) r1.value()).values()).value());

        var r2 = parser.parse("deleted = FALSE");
        assertOk(r2);
        assertEquals(Boolean.FALSE, ((Values.Single) ((ColumnFilter) r2.value()).values()).value());

        var r3 = parser.parse("note = NULL");
        assertOk(r3);
        assertNull(((Values.Single) ((ColumnFilter) r3.value()).values()).value());
    }

    /* ------------------------------ errors ---------------------------------- */

    @Test
    @DisplayName("Unterminated string -> error")
    void unterminated_string_error() {
        var r = parser.parse("name LIKE 'abc");
        assertFalse(r.ok());
        assertFalse(r.problems().isEmpty());
    }

    @Test
    @DisplayName("Tuple must have at least two items")
    void tuple_arity_error() {
        var r = parser.parse("(a) IN ((1))");
        assertFalse(r.ok());
    }

    @Test
    @DisplayName("Unexpected trailing input after a valid filter")
    void trailing_input_error() {
        var r = parser.parse("qty = 5 garbage");
        assertFalse(r.ok());
    }

    /* ============================== helpers ============================== */

    private static void assertOk(ParseResult<?> r) {
        assertTrue(r.ok(), () -> "problems: " + r.problems());
        assertNotNull(r.value());
        assertFalse(r.value() instanceof ExpressionFilter);
    }

    private static <T> T assertIs(Class<T> type, Object value) {
        assertNotNull(value, "value is null");
        assertTrue(type.isInstance(value), () -> "Expected " + type.getSimpleName() + " but was " + value.getClass().getSimpleName());
        return type.cast(value);
    }
}
