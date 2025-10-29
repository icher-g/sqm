package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.PredicateParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
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
class PredicateParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final PredicateParser parser = new PredicateParser();

    /* ------------------------ single-column operators ------------------------ */

    private static void assertOk(ParseResult<?> r) {
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Assertions.assertNotNull(r.value());
    }

    private static <T> T assertIs(Class<T> type, Object value) {
        Assertions.assertNotNull(value, "value is null");
        Assertions.assertTrue(type.isInstance(value), () -> "Expected " + type.getSimpleName() + " but was " + value.getClass().getSimpleName());
        return type.cast(value);
    }

    @Test
    @DisplayName("category IN (1, 2, 3)")
    void in_list() {
        var r = parser.parse("category IN (1, 2, 3)", ctx);
        assertOk(r);
        var f = assertIs(InPredicate.class, r.value());
        var v = assertIs(RowExpr.class, f.rhs());
        Assertions.assertEquals(List.of(1L, 2L, 3L), v.items().stream().map(i -> i.asLiteral().value()).toList()); // longs if your parser maps integers to Long
        Assertions.assertEquals("category", f.lhs().asColumn().name());
    }

    @Test
    @DisplayName("status NOT IN ('A','B')")
    void not_in_list() {
        var r = parser.parse("status NOT IN ('A','B')", ctx);
        assertOk(r);
        var f = assertIs(InPredicate.class, r.value());
        Assertions.assertTrue(f.negated());
        var v = assertIs(RowExpr.class, f.rhs());
        Assertions.assertEquals(List.of("A", "B"), v.items().stream().map(i -> i.asLiteral().value()).toList());
        Assertions.assertEquals("status", f.lhs().asColumn().name());
    }

    @Test
    @DisplayName("name LIKE '%abc%'")
    void like_single() {
        var r = parser.parse("name LIKE '%abc%'", ctx);
        assertOk(r);
        var f = assertIs(LikePredicate.class, r.value());
        var v = assertIs(LiteralExpr.class, f.pattern());
        Assertions.assertEquals("%abc%", v.value());
        Assertions.assertEquals("name", f.value().asColumn().name());
    }

    /* ----------------------------- tuple IN --------------------------------- */

    @Test
    @DisplayName("price BETWEEN 10 AND 20")
    void between_range() {
        var r = parser.parse("price BETWEEN 10 AND 20", ctx);
        assertOk(r);
        var f = assertIs(BetweenPredicate.class, r.value());
        var l = assertIs(LiteralExpr.class, f.lower());
        var u = assertIs(LiteralExpr.class, f.upper());
        Assertions.assertEquals(10L, l.value());
        Assertions.assertEquals(20L, u.value());
        Assertions.assertEquals("price", f.value().asColumn().name());
    }

    /* --------------------- boolean composition & precedence ------------------ */

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "qty = 5,EQ,5",
        "qty != 5,NE,5",
        "qty <> 5,NE,5",
        "qty > 5,GT,5",
        "qty >= 5,GTE,5",
        "qty < 5,LT,5",
        "qty <= 5,LTE,5"
    })
    @DisplayName("Comparison operators map to ComparisonPredicate")
    void comparisons(String expr, ComparisonOperator operator, long expected) {
        var r = parser.parse(expr, ctx);
        assertOk(r);
        var f = assertIs(ComparisonPredicate.class, r.value());
        Assertions.assertEquals(operator, f.operator());
        var v = assertIs(LiteralExpr.class, f.rhs());
        Assertions.assertEquals(expected, v.value());
        Assertions.assertEquals("qty", f.lhs().asColumn().name());
    }

    @Test
    @DisplayName("Tuple IN: (a,b) IN ((1,2),(3,4)) builds TupleColumnFilter + Values.Tuples")
    void tuple_in() {
        var r = parser.parse("(a, b) IN ((1,2), (3,4))", ctx);
        assertOk(r);
        var f = assertIs(InPredicate.class, r.value());
        var c0 = f.lhs().asValues().asRow().items().get(0);
        var c1 = f.lhs().asValues().asRow().items().get(1);
        Assertions.assertEquals(2, f.lhs().asValues().asRow().items().size());
        Assertions.assertEquals("a", c0.asColumn().name());
        Assertions.assertEquals("b", c1.asColumn().name());

        var v = assertIs(RowListExpr.class, f.rhs());
        Assertions.assertEquals(List.of(List.of(1L, 2L), List.of(3L, 4L)),
            v.rows().stream().map(row -> row.items().stream().map(i -> i.asLiteral().value()).toList()).toList());
    }

    /* ------------------------------ literals -------------------------------- */

    @Test
    @DisplayName("AND has higher precedence than OR when grouped by parser (NOT > AND > OR)")
    void boolean_precedence_and_grouping() {
        var r = parser.parse("status IN ('A','B') AND (price BETWEEN 10 AND 20 OR name NOT LIKE '%test%')", ctx);
        assertOk(r);

        var root = assertIs(AndPredicate.class, r.value());
        // left is ColumnFilter IN
        assertIs(InPredicate.class, root.lhs());
        // right is ( ... OR NOT ... )
        var right = assertIs(OrPredicate.class, root.rhs());
        assertIs(LikePredicate.class, right.rhs());
        assertIs(BetweenPredicate.class, right.lhs());
    }

    @Test
    @DisplayName("NOT binds tighter than AND/OR; parentheses preserved")
    void boolean_not_precedence() {
        var r = parser.parse("status NOT IN ('X') OR price >= 100", ctx);
        assertOk(r);

        var or = assertIs(OrPredicate.class, r.value());
        var left = assertIs(InPredicate.class, or.lhs());
        var right = assertIs(ComparisonPredicate.class, or.rhs());
        assertIs(ColumnExpr.class, left.lhs());
        assertIs(RowExpr.class, left.rhs());
        assertIs(ColumnExpr.class, right.lhs());
        assertIs(LiteralExpr.class, right.rhs());
    }

    /* ------------------------------ errors ---------------------------------- */

    @Test
    @DisplayName("Escaped quote in string literal: 'O''Reilly'")
    void escaped_quotes() {
        var r = parser.parse("name LIKE 'O''Reilly'", ctx);
        assertOk(r);
        var f = assertIs(LikePredicate.class, r.value());
        var v = assertIs(LiteralExpr.class, f.pattern());
        Assertions.assertEquals("O'Reilly", v.value());
    }

    @Test
    @DisplayName("Booleans and NULL literals")
    void boolean_and_null_literals() {
        var r1 = parser.parse("active = TRUE", ctx);
        assertOk(r1);
        Assertions.assertEquals(Boolean.TRUE, r1.value().asPredicate().asComparison().rhs().asLiteral().value());

        var r2 = parser.parse("deleted = FALSE", ctx);
        assertOk(r2);
        Assertions.assertEquals(Boolean.FALSE, r2.value().asPredicate().asComparison().rhs().asLiteral().value());

        var r3 = parser.parse("note = NULL", ctx);
        assertOk(r3);
        Assertions.assertNull(r3.value().asPredicate().asComparison().rhs().asLiteral().value());
    }

    @Test
    @DisplayName("Unterminated string -> error")
    void unterminated_string_error() {
        var r = parser.parse("name LIKE 'abc", ctx);
        Assertions.assertFalse(r.ok());
        Assertions.assertFalse(r.problems().isEmpty());
    }

    @Test
    @DisplayName("(a) IN ((1))")
    void tuple_arity_error() {
        var r = parser.parse("(a) IN ((1))", ctx);
        Assertions.assertTrue(r.ok());
        Assertions.assertInstanceOf(InPredicate.class, r.value());
        Assertions.assertInstanceOf(RowExpr.class, r.value().asIn().lhs());
        Assertions.assertInstanceOf(RowListExpr.class, r.value().asIn().rhs());
    }

    @Test
    @DisplayName("Unexpected trailing input after a valid filter")
    void trailing_input_error() {
        var r = parser.parse("qty = 5 garbage", ctx);
        Assertions.assertFalse(r.ok());
    }
}
