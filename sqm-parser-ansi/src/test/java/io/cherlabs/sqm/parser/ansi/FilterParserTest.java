package io.cherlabs.sqm.parser.ansi;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.parser.FilterParser;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

/**
 * Tests for FilterSpecParser:
 * - single-column operators: IN / NOT IN / LIKE / BETWEEN / = != <> > >= < <=
 * - tuple IN: (a,b) IN ((1,2),(3,4))
 * - boolean composition with precedence and grouping: NOT > AND > OR
 * - escaped quotes in strings
 * - error handling
 * Adjust assertions if your concrete type names differ.
 */
class FilterParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final FilterParser parser = new FilterParser();

    /* ------------------------ single-column operators ------------------------ */

    private static void assertOk(ParseResult<?> r) {
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Assertions.assertNotNull(r.value());
        Assertions.assertFalse(r.value() instanceof ExpressionFilter);
    }

    private static <T> T assertIs(Class<T> type, Object value) {
        Assertions.assertNotNull(value, "value is null");
        Assertions.assertTrue(type.isInstance(value), () -> "Expected " + type.getSimpleName() + " but was " + value.getClass().getSimpleName());
        return type.cast(value);
    }

    @Test
    @DisplayName("IN builds ColumnFilter with Values.ListVals")
    void in_list() {
        var r = parser.parse("category IN (1, 2, 3)", ctx);
        assertOk(r);
        var f = assertIs(ColumnFilter.class, r.value());
        Assertions.assertEquals(ColumnFilter.Operator.In, f.op());
        var v = assertIs(Values.ListValues.class, f.values());
        Assertions.assertEquals(List.of(1L, 2L, 3L), v.items()); // longs if your parser maps integers to Long
        Assertions.assertEquals("category", f.columnAs(NamedColumn.class).name());
    }

    @Test
    @DisplayName("NOT IN builds ColumnFilter with Values.ListVals")
    void not_in_list() {
        var r = parser.parse("status NOT IN ('A','B')", ctx);
        assertOk(r);
        var f = assertIs(ColumnFilter.class, r.value());
        Assertions.assertEquals(ColumnFilter.Operator.NotIn, f.op());
        var v = assertIs(Values.ListValues.class, f.values());
        Assertions.assertEquals(List.of("A", "B"), v.items());
        Assertions.assertEquals("status", f.columnAs(NamedColumn.class).name());
    }

    @Test
    @DisplayName("LIKE builds ColumnFilter with Values.Single")
    void like_single() {
        var r = parser.parse("name LIKE '%abc%'", ctx);
        assertOk(r);
        var f = assertIs(ColumnFilter.class, r.value());
        Assertions.assertEquals(ColumnFilter.Operator.Like, f.op());
        var v = assertIs(Values.Single.class, f.values());
        Assertions.assertEquals("%abc%", v.value());
        Assertions.assertEquals("name", f.columnAs(NamedColumn.class).name());
    }

    /* ----------------------------- tuple IN --------------------------------- */

    @Test
    @DisplayName("BETWEEN builds ColumnFilter with Values.Range")
    void between_range() {
        var r = parser.parse("price BETWEEN 10 AND 20", ctx);
        assertOk(r);
        var f = assertIs(ColumnFilter.class, r.value());
        Assertions.assertEquals(ColumnFilter.Operator.Range, f.op());
        var v = assertIs(Values.Range.class, f.values());
        Assertions.assertEquals(10L, v.min());
        Assertions.assertEquals(20L, v.max());
        Assertions.assertEquals("price", f.columnAs(NamedColumn.class).name());
    }

    /* --------------------- boolean composition & precedence ------------------ */

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
        var r = parser.parse(expr, ctx);
        assertOk(r);
        var f = assertIs(ColumnFilter.class, r.value());
        Assertions.assertEquals(operator, f.op());
        var v = assertIs(Values.Single.class, f.values());
        Assertions.assertEquals(expected, v.value());
        Assertions.assertEquals("qty", f.columnAs(NamedColumn.class).name());
    }

    @Test
    @DisplayName("Tuple IN: (a,b) IN ((1,2),(3,4)) builds TupleColumnFilter + Values.Tuples")
    void tuple_in() {
        var r = parser.parse("(a, b) IN ((1,2), (3,4))", ctx);
        assertOk(r);
        var f = assertIs(TupleFilter.class, r.value());
        var c0 = (NamedColumn) f.columns().get(0);
        var c1 = (NamedColumn) f.columns().get(1);
        Assertions.assertEquals(TupleFilter.Operator.In, f.operator());
        Assertions.assertEquals(2, f.columns().size());
        Assertions.assertEquals("a", c0.name());
        Assertions.assertEquals("b", c1.name());

        var v = assertIs(Values.Tuples.class, f.values());
        Assertions.assertEquals(List.of(List.of(1L, 2L), List.of(3L, 4L)), v.rows());
    }

    /* ------------------------------ literals -------------------------------- */

    @Test
    @DisplayName("AND has higher precedence than OR when grouped by parser (NOT > AND > OR)")
    void boolean_precedence_and_grouping() {
        var r = parser.parse("status IN ('A','B') AND (price BETWEEN 10 AND 20 OR NOT name LIKE '%test%')", ctx);
        assertOk(r);

        var root = assertIs(CompositeFilter.class, r.value());
        Assertions.assertEquals(CompositeFilter.Operator.And, root.op());
        Assertions.assertEquals(2, root.filters().size());

        // left is ColumnFilter IN
        assertIs(ColumnFilter.class, root.filters().get(0));

        // right is ( ... OR NOT ... )
        var right = assertIs(CompositeFilter.class, root.filters().get(1));
        Assertions.assertEquals(CompositeFilter.Operator.Or, right.op());
        Assertions.assertEquals(2, right.filters().size());

        var not = assertIs(CompositeFilter.class, right.filters().get(1));
        Assertions.assertEquals(CompositeFilter.Operator.Not, not.op());
        Assertions.assertEquals(1, not.filters().size());
        assertIs(ColumnFilter.class, not.filters().get(0));
    }

    @Test
    @DisplayName("NOT binds tighter than AND/OR; parentheses preserved")
    void boolean_not_precedence() {
        var r = parser.parse("NOT status IN ('X') OR price >= 100", ctx);
        assertOk(r);

        var or = assertIs(CompositeFilter.class, r.value());
        Assertions.assertEquals(CompositeFilter.Operator.Or, or.op());
        Assertions.assertEquals(2, or.filters().size());

        var left = assertIs(CompositeFilter.class, or.filters().get(0));
        Assertions.assertEquals(CompositeFilter.Operator.Not, left.op());
        assertIs(ColumnFilter.class, left.filters().get(0));
    }

    /* ------------------------------ errors ---------------------------------- */

    @Test
    @DisplayName("Escaped quote in string literal: 'O''Reilly'")
    void escaped_quotes() {
        var r = parser.parse("name LIKE 'O''Reilly'", ctx);
        assertOk(r);
        var f = assertIs(ColumnFilter.class, r.value());
        var v = assertIs(Values.Single.class, f.values());
        Assertions.assertEquals("O'Reilly", v.value());
    }

    @Test
    @DisplayName("Booleans and NULL literals")
    void boolean_and_null_literals() {
        var r1 = parser.parse("active = TRUE", ctx);
        assertOk(r1);
        Assertions.assertEquals(Boolean.TRUE, ((Values.Single) ((ColumnFilter) r1.value()).values()).value());

        var r2 = parser.parse("deleted = FALSE", ctx);
        assertOk(r2);
        Assertions.assertEquals(Boolean.FALSE, ((Values.Single) ((ColumnFilter) r2.value()).values()).value());

        var r3 = parser.parse("note = NULL", ctx);
        assertOk(r3);
        Assertions.assertNull(((Values.Single) ((ColumnFilter) r3.value()).values()).value());
    }

    @Test
    @DisplayName("Unterminated string -> error")
    void unterminated_string_error() {
        var r = parser.parse("name LIKE 'abc", ctx);
        Assertions.assertFalse(r.ok());
        Assertions.assertFalse(r.problems().isEmpty());
    }

    /* ============================== helpers ============================== */

    @Test
    @DisplayName("Tuple must have at least two items")
    void tuple_arity_error() {
        var r = parser.parse("(a) IN ((1))", ctx);
        Assertions.assertFalse(r.ok());
    }

    @Test
    @DisplayName("Unexpected trailing input after a valid filter")
    void trailing_input_error() {
        var r = parser.parse("qty = 5 garbage", ctx);
        Assertions.assertFalse(r.ok());
    }
}
