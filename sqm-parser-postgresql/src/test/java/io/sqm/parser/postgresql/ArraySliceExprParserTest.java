package io.sqm.parser.postgresql;

import io.sqm.core.*;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for PostgreSQL {@link ArraySliceExpr}.
 *
 * <p>Tests array slicing expressions of the form {@code expr[from:to]}.</p>
 */
@DisplayName("PostgreSQL ArraySliceExprParser Tests")
class ArraySliceExprParserTest {

    private final ParseContext parseContext = ParseContext.of(new PostgresSpecs());

    @Test
    void parsesArraySlice() {
        var result = parseExpr("arr[1:3]");
        assertTrue(result.ok());
        var expr = result.value();
        assertInstanceOf(ArraySliceExpr.class, expr);
    }

    @Test
    @DisplayName("Parse array slice with both bounds")
    void parsesArraySliceWithBothBounds() {
        var result = parseExpr("arr[2:5]");

        assertTrue(result.ok());
        var slice = assertInstanceOf(ArraySliceExpr.class, result.value());

        assertInstanceOf(ColumnExpr.class, slice.base());
        assertTrue(slice.from().isPresent());
        assertTrue(slice.to().isPresent());

        var from = assertInstanceOf(LiteralExpr.class, slice.from().get());
        assertEquals(2L, from.value());

        var to = assertInstanceOf(LiteralExpr.class, slice.to().get());
        assertEquals(5L, to.value());
    }

    @Test
    @DisplayName("Parse array slice with missing from bound")
    void parsesArraySliceWithMissingFrom() {
        var result = parseExpr("arr[:5]");

        assertTrue(result.ok());
        var slice = assertInstanceOf(ArraySliceExpr.class, result.value());

        assertInstanceOf(ColumnExpr.class, slice.base());
        assertTrue(slice.from().isEmpty());
        assertTrue(slice.to().isPresent());
    }

    @Test
    @DisplayName("Parse array slice with missing to bound")
    void parsesArraySliceWithMissingTo() {
        var result = parseExpr("arr[2:]");

        assertTrue(result.ok());
        var slice = assertInstanceOf(ArraySliceExpr.class, result.value());

        assertInstanceOf(ColumnExpr.class, slice.base());
        assertTrue(slice.from().isPresent());
        assertTrue(slice.to().isEmpty());
    }

    @Test
    @DisplayName("Parse array slice with both bounds missing")
    void parsesArraySliceWithBothBoundsMissing() {
        var result = parseExpr("arr[:]");

        assertTrue(result.ok());
        var slice = assertInstanceOf(ArraySliceExpr.class, result.value());

        assertInstanceOf(ColumnExpr.class, slice.base());
        assertTrue(slice.from().isEmpty());
        assertTrue(slice.to().isEmpty());
    }

    @Test
    @DisplayName("Parse array slice with column expressions as bounds")
    void parsesArraySliceWithColumnBounds() {
        var result = parseExpr("arr[start_idx:end_idx]");

        assertTrue(result.ok());
        var slice = assertInstanceOf(ArraySliceExpr.class, result.value());

        assertTrue(slice.from().isPresent());
        assertTrue(slice.to().isPresent());

        assertInstanceOf(ColumnExpr.class, slice.from().get());
        assertInstanceOf(ColumnExpr.class, slice.to().get());
    }

    @Test
    @DisplayName("Parse array slice with arithmetic expressions as bounds")
    void parsesArraySliceWithArithmeticBounds() {
        var result = parseExpr("arr[i+1:i+5]");

        assertTrue(result.ok());
        var slice = assertInstanceOf(ArraySliceExpr.class, result.value());

        assertTrue(slice.from().isPresent());
        assertTrue(slice.to().isPresent());

        assertInstanceOf(AddArithmeticExpr.class, slice.from().get());
        assertInstanceOf(AddArithmeticExpr.class, slice.to().get());
    }

    @Test
    @DisplayName("Parse array slice in WHERE clause")
    void parsesArraySliceInWhereClause() {
        var qResult = parseQuery("SELECT * FROM t WHERE tags[1:3] = ARRAY['a','b','c']");

        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.where());

        var pred = assertInstanceOf(ComparisonPredicate.class, query.where());
        assertInstanceOf(ArraySliceExpr.class, pred.lhs());
    }

    @Test
    @DisplayName("Parse array slice in SELECT list")
    void parsesArraySliceInSelectList() {
        var qResult = parseQuery("SELECT arr[1:3] AS slice FROM t");

        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());

        var item = assertInstanceOf(ExprSelectItem.class, query.items().getFirst());
        assertInstanceOf(ArraySliceExpr.class, item.expr());
        assertNotNull(item.alias());
        assertEquals("slice", item.alias().value());
    }

    @Test
    @DisplayName("Parse qualified column with array slice")
    void parsesQualifiedColumnWithSlice() {
        var result = parseExpr("t.arr[2:5]");

        assertTrue(result.ok());
        var slice = assertInstanceOf(ArraySliceExpr.class, result.value());

        var base = assertInstanceOf(ColumnExpr.class, slice.base());
        assertEquals("t", base.tableAlias().value());
        assertEquals("arr", base.name().value());
    }

    @Test
    @DisplayName("Parse array constructor with slice")
    void parsesArrayConstructorWithSlice() {
        var result = parseExpr("ARRAY[1,2,3,4,5][2:4]");

        assertTrue(result.ok());
        var slice = assertInstanceOf(ArraySliceExpr.class, result.value());

        assertInstanceOf(ArrayExpr.class, slice.base());
    }

    @Test
    @DisplayName("Parse negative bounds")
    void parsesNegativeBounds() {
        var result = parseExpr("arr[-3:-1]");

        assertTrue(result.ok());
        var slice = assertInstanceOf(ArraySliceExpr.class, result.value());

        assertTrue(slice.from().isPresent());
        assertTrue(slice.to().isPresent());

        assertInstanceOf(NegativeArithmeticExpr.class, slice.from().get());
        assertInstanceOf(NegativeArithmeticExpr.class, slice.to().get());
    }

    @Test
    @DisplayName("Reject array slice with extra colon")
    void rejectsArraySliceWithExtraColon() {
        assertParseError("arr[1:2:3]");
    }

    @Test
    @DisplayName("Reject array slice missing closing bracket")
    void rejectsArraySliceMissingClosingBracket() {
        assertParseError("arr[1:2");
    }

    private ParseResult<? extends Expression> parseExpr(String sql) {
        return parseContext.parse(Expression.class, sql);
    }

    private ParseResult<? extends Query> parseQuery(String sql) {
        return parseContext.parse(Query.class, sql);
    }

    private void assertParseError(String sql) {
        var result = parseExpr(sql);
        if (result.ok()) {
            fail("Expected parse error for: " + sql);
        }
    }
}
