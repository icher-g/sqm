package io.sqm.parser.postgresql;

import io.sqm.core.*;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for PostgreSQL {@link CastExpr}.
 *
 * <p>These tests verify parsing of both:</p>
 * <ul>
 *   <li>ANSI syntax: {@code CAST(expr AS type)}</li>
 *   <li>PostgreSQL shorthand: {@code expr::type}</li>
 * </ul>
 */
@DisplayName("PostgreSQL CastExprParser Tests")
class CastExprParserTest {

    private final ParseContext parseContext = ParseContext.of(new PostgresSpecs());

    @Test
    @DisplayName("Parse PostgreSQL cast shorthand: column::type")
    void parsesPostgresCastShorthand() {
        var result = parseExpr("a::int");
        
        assertTrue(result.ok());
        var cast = assertInstanceOf(CastExpr.class, result.value());
        assertInstanceOf(ColumnExpr.class, cast.expr());
        
        var type = cast.type();
        assertEquals(java.util.List.of("int"), type.qualifiedName());
    }

    @Test
    @DisplayName("Parse PostgreSQL cast with qualified type name")
    void parsesPostgresCastWithQualifiedType() {
        var result = parseExpr("value::pg_catalog.int4");
        
        assertTrue(result.ok());
        var cast = assertInstanceOf(CastExpr.class, result.value());
        
        var type = cast.type();
        assertEquals(java.util.List.of("pg_catalog", "int4"), type.qualifiedName());
    }

    @Test
    @DisplayName("Parse PostgreSQL cast with array type")
    void parsesPostgresCastWithArrayType() {
        var result = parseExpr("vals::text[]");
        
        assertTrue(result.ok());
        var cast = assertInstanceOf(CastExpr.class, result.value());
        
        var type = cast.type();
        assertEquals(java.util.List.of("text"), type.qualifiedName());
        assertEquals(1, type.arrayDims());
    }

    @Test
    @DisplayName("Parse PostgreSQL cast with multi-dimensional array")
    void parsesPostgresCastWithMultiDimArray() {
        var result = parseExpr("matrix::int[][]");
        
        assertTrue(result.ok());
        var cast = assertInstanceOf(CastExpr.class, result.value());
        
        var type = cast.type();
        assertEquals(java.util.List.of("int"), type.qualifiedName());
        assertEquals(2, type.arrayDims());
    }

    @Test
    @DisplayName("Parse ANSI CAST syntax still works")
    void parsesAnsiCastSyntax() {
        var result = parseExpr("CAST(a AS varchar(10))");
        
        assertTrue(result.ok());
        var cast = assertInstanceOf(CastExpr.class, result.value());
        assertInstanceOf(ColumnExpr.class, cast.expr());
        
        var type = cast.type();
        assertEquals(java.util.List.of("varchar"), type.qualifiedName());
        assertEquals(1, type.modifiers().size());
    }

    @Test
    @DisplayName("Parse cast with literal expression")
    void parsesCastWithLiteral() {
        var result = parseExpr("'2024-01-01'::date");
        
        assertTrue(result.ok());
        var cast = assertInstanceOf(CastExpr.class, result.value());
        assertInstanceOf(LiteralExpr.class, cast.expr());
    }

    @Test
    @DisplayName("Parse cast with function expression")
    void parsesCastWithFunction() {
        var result = parseExpr("now()::text");
        
        assertTrue(result.ok());
        var cast = assertInstanceOf(CastExpr.class, result.value());
        assertInstanceOf(FunctionExpr.class, cast.expr());
    }

    @Test
    @DisplayName("Parse cast with nested cast")
    void parsesNestedCast() {
        var result = parseExpr("value::text::varchar(10)");
        
        assertTrue(result.ok());
        var cast1 = assertInstanceOf(CastExpr.class, result.value());
        var cast2 = assertInstanceOf(CastExpr.class, cast1.expr());
        assertInstanceOf(ColumnExpr.class, cast2.expr());
    }

    @Test
    @DisplayName("Parse cast in WHERE clause")
    void parsesCastInWhereClause() {
        var qResult = parseQuery("SELECT * FROM t WHERE id::text = '123'");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.where());
        
        var pred = assertInstanceOf(ComparisonPredicate.class, query.where());
        assertInstanceOf(CastExpr.class, pred.lhs());
    }

    @Test
    @DisplayName("Parse cast in SELECT list")
    void parsesCastInSelectList() {
        var qResult = parseQuery("SELECT id::text AS id_str FROM t");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        var item = assertInstanceOf(ExprSelectItem.class, query.items().getFirst());
        assertInstanceOf(CastExpr.class, item.expr());
        assertNotNull(item.alias());
        assertEquals("id_str", item.alias());
    }

    private ParseResult<? extends Expression> parseExpr(String sql) {
        return parseContext.parse(Expression.class, sql);
    }

    private ParseResult<? extends Query> parseQuery(String sql) {
        return parseContext.parse(Query.class, sql);
    }
}
