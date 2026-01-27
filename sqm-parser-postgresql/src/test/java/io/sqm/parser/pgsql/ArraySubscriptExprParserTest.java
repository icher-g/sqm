package io.sqm.parser.pgsql;

import io.sqm.core.*;
import io.sqm.parser.pgsql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for PostgreSQL {@link ArraySubscriptExpr}.
 *
 * <p>Tests array subscripting expressions of the form {@code expr[index]}.</p>
 */
@DisplayName("PostgreSQL ArraySubscriptExprParser Tests")
class ArraySubscriptExprParserTest {

    private final ParseContext parseContext = ParseContext.of(new PostgresSpecs());

    @Test
    @DisplayName("Parse simple array subscript on column")
    void parsesSimpleArraySubscript() {
        var result = parseExpr("arr[1]");
        
        assertTrue(result.ok());
        var sub = assertInstanceOf(ArraySubscriptExpr.class, result.value());
        
        assertInstanceOf(ColumnExpr.class, sub.base());
        assertInstanceOf(LiteralExpr.class, sub.index());
        
        var index = (LiteralExpr) sub.index();
        assertEquals(1L, index.value());
    }

    @Test
    @DisplayName("Parse array subscript with column as index")
    void parsesArraySubscriptWithColumnIndex() {
        var result = parseExpr("arr[i]");
        
        assertTrue(result.ok());
        var sub = assertInstanceOf(ArraySubscriptExpr.class, result.value());
        
        assertInstanceOf(ColumnExpr.class, sub.base());
        assertInstanceOf(ColumnExpr.class, sub.index());
    }

    @Test
    @DisplayName("Parse array subscript with expression as index")
    void parsesArraySubscriptWithExpressionIndex() {
        var result = parseExpr("arr[i + 1]");
        
        assertTrue(result.ok());
        var sub = assertInstanceOf(ArraySubscriptExpr.class, result.value());
        
        assertInstanceOf(ColumnExpr.class, sub.base());
        assertInstanceOf(AddArithmeticExpr.class, sub.index());
    }

    @Test
    @DisplayName("Parse chained array subscripts")
    void parsesChainedArraySubscripts() {
        var result = parseExpr("matrix[1][2]");
        
        assertTrue(result.ok());
        var sub2 = assertInstanceOf(ArraySubscriptExpr.class, result.value());
        var sub1 = assertInstanceOf(ArraySubscriptExpr.class, sub2.base());
        
        assertInstanceOf(ColumnExpr.class, sub1.base());
        var col = (ColumnExpr) sub1.base();
        assertEquals("matrix", col.name());
    }

    @Test
    @DisplayName("Parse three-level nested subscripts")
    void parsesThreeLevelSubscripts() {
        var result = parseExpr("arr[1][2][3]");
        
        assertTrue(result.ok());
        var sub3 = assertInstanceOf(ArraySubscriptExpr.class, result.value());
        var sub2 = assertInstanceOf(ArraySubscriptExpr.class, sub3.base());
        var sub1 = assertInstanceOf(ArraySubscriptExpr.class, sub2.base());
        
        assertInstanceOf(ColumnExpr.class, sub1.base());
    }

    @Test
    @DisplayName("Parse array constructor with subscript")
    void parsesArrayConstructorWithSubscript() {
        var result = parseExpr("ARRAY[1,2,3][2]");
        
        assertTrue(result.ok());
        var sub = assertInstanceOf(ArraySubscriptExpr.class, result.value());
        
        assertInstanceOf(ArrayExpr.class, sub.base());
    }

    @Test
    @DisplayName("Parse array subscript in WHERE clause")
    void parsesArraySubscriptInWhereClause() {
        var qResult = parseQuery("SELECT * FROM t WHERE tags[1] = 'important'");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.where());
        
        var pred = assertInstanceOf(ComparisonPredicate.class, query.where());
        assertInstanceOf(ArraySubscriptExpr.class, pred.lhs());
    }

    @Test
    @DisplayName("Parse array subscript in SELECT list")
    void parsesArraySubscriptInSelectList() {
        var qResult = parseQuery("SELECT arr[1] AS first_elem FROM t");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        var item = assertInstanceOf(ExprSelectItem.class, query.items().getFirst());
        assertInstanceOf(ArraySubscriptExpr.class, item.expr());
        assertNotNull(item.alias());
        assertEquals("first_elem", item.alias());
    }

    @Test
    @DisplayName("Parse qualified column with array subscript")
    void parsesQualifiedColumnWithSubscript() {
        var result = parseExpr("t.arr[1]");
        
        assertTrue(result.ok());
        var sub = assertInstanceOf(ArraySubscriptExpr.class, result.value());
        
        var base = assertInstanceOf(ColumnExpr.class, sub.base());
        assertEquals("t", base.tableAlias());
        assertEquals("arr", base.name());
    }

    @Test
    @DisplayName("Parse negative index")
    void parsesNegativeIndex() {
        var result = parseExpr("arr[-1]");
        
        assertTrue(result.ok());
        var sub = assertInstanceOf(ArraySubscriptExpr.class, result.value());
        
        assertInstanceOf(NegativeArithmeticExpr.class, sub.index());
    }

    private ParseResult<? extends Expression> parseExpr(String sql) {
        return parseContext.parse(Expression.class, sql);
    }

    private ParseResult<? extends Query> parseQuery(String sql) {
        return parseContext.parse(Query.class, sql);
    }
}
