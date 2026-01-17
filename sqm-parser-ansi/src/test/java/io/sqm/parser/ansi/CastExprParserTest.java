package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for {@link CastExpr}.
 *
 * <p>These tests verify parsing of {@code CAST(expr AS type)} including:</p>
 * <ul>
 *   <li>qualified type names (dot-separated identifiers)</li>
 *   <li>keyword-based types such as {@code DOUBLE PRECISION}</li>
 *   <li>type modifiers such as {@code numeric(10,2)}</li>
 * </ul>
 *
 * <p>ANSI parser is expected to reject PostgreSQL-specific extensions such as:</p>
 * <ul>
 *   <li>array type syntax {@code []}</li>
 *   <li>type time zone clauses {@code WITH/WITHOUT TIME ZONE}</li>
 * </ul>
 */
class CastExprParserTest {

    @Test
    void parsesSimpleCast() {
        var e = parseExpr("CAST(a AS int)");

        var cast = assertInstanceOf(CastExpr.class, e.value());
        assertInstanceOf(ColumnExpr.class, cast.expr());

        var type = cast.type();
        assertTrue(type.keyword().isEmpty());
        assertEquals(java.util.List.of("int"), type.qualifiedName());
        assertTrue(type.modifiers().isEmpty());
        assertEquals(0, type.arrayDims());
        assertEquals(TimeZoneSpec.NONE, type.timeZoneSpec());
    }

    @Test
    void parsesQualifiedTypeName() {
        var e = parseExpr("CAST(a AS pg_catalog.int4)");

        var cast = assertInstanceOf(CastExpr.class, e.value());

        var type = cast.type();
        assertTrue(type.keyword().isEmpty());
        assertEquals(java.util.List.of("pg_catalog", "int4"), type.qualifiedName());
    }

    @Test
    void parsesTypeWithSingleModifier() {
        var e = parseExpr("CAST(a AS varchar(10))");

        var cast = assertInstanceOf(CastExpr.class, e.value());

        var type = cast.type();
        assertEquals(java.util.List.of("varchar"), type.qualifiedName());
        assertEquals(1, type.modifiers().size());
    }

    @Test
    void parsesTypeWithMultipleModifiers() {
        var e = parseExpr("CAST(a AS numeric(10,2))");

        var cast = assertInstanceOf(CastExpr.class, e.value());

        var type = cast.type();
        assertEquals(java.util.List.of("numeric"), type.qualifiedName());
        assertEquals(2, type.modifiers().size());
    }

    @Test
    void parsesKeywordType_doublePrecision() {
        var e = parseExpr("CAST(a AS DOUBLE PRECISION)");

        var cast = assertInstanceOf(CastExpr.class, e.value());

        var type = cast.type();
        assertTrue(type.qualifiedName().isEmpty());
        assertEquals(TypeKeyword.DOUBLE_PRECISION, type.keyword().orElseThrow());
        assertTrue(type.modifiers().isEmpty());
        assertEquals(TimeZoneSpec.NONE, type.timeZoneSpec());
    }

    @Test
    void parsesCastInSelectList() {
        var q = parseQuery("""
            select CAST(a AS numeric(10,2)) as x
            from t
            """);

        // Adapt these assertions to your query model, but keep the semantic checks:
        // Ensure there is a CastExpr in the select list and its TypeName is numeric(10,2).
        var select = assertInstanceOf(SelectQuery.class, q);

        var item = select.items().getFirst();
        var exprItem = assertInstanceOf(ExprSelectItem.class, item);

        var cast = assertInstanceOf(CastExpr.class, exprItem.expr());
        assertEquals(java.util.List.of("numeric"), cast.type().qualifiedName());
        assertEquals(2, cast.type().modifiers().size());
    }

    @Test
    void parsesCastInWherePredicate() {
        var q = parseQuery("""
            select *
            from t
            where CAST(a AS int) = 1
            """);

        // Adapt to your predicate model:
        // Assert there is a ComparisonPredicate where lhs is CastExpr and rhs is a literal.
        var select = assertInstanceOf(SelectQuery.class, q);

        var where = select.where();
        var cmp = assertInstanceOf(ComparisonPredicate.class, where);

        var lhs = assertInstanceOf(CastExpr.class, cmp.lhs());
        assertEquals(java.util.List.of("int"), lhs.type().qualifiedName());
    }

    @Test
    void rejectsCastMissingAsKeyword() {
        assertParseError("CAST(a int)");
    }

    @Test
    void rejectsCastMissingType() {
        assertParseError("CAST(a AS)");
    }

    @Test
    void rejectsArrayTypeInAnsiParser() {
        assertParseError("CAST(a AS text[])");
    }

    @Test
    void rejectsTimeZoneClauseInAnsiParser() {
        assertParseError("CAST(a AS timestamp with time zone)");
    }

    @Test
    void rejectsTimeZoneClauseInAnsiParser_without() {
        assertParseError("CAST(a AS timestamp without time zone)");
    }

    // ---------------------------------------------------------------------
    // Test harness hooks
    // ---------------------------------------------------------------------

    /**
     * Parses a SQL expression using the ANSI parser entry point.
     *
     * <p>Wire this method to your actual ANSI expression parser, for example:</p>
     * <ul>
     *   <li>{@code return new AnsiParser().parseExpression(sql);}</li>
     *   <li>or your existing {@code ParserTestSupport.parseExpr(sql)}</li>
     * </ul>
     */
    private ParseResult<? extends Expression> parseExpr(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        return ctx.parse(Expression.class, sql);
    }

    /**
     * Parses a full SQL query using the ANSI parser entry point.
     *
     * <p>Wire this method to your actual ANSI query parser entry point.</p>
     */
    private Query parseQuery(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        return ctx.parse(Query.class, sql).value();
    }

    /**
     * Asserts that parsing the given SQL fails with a parse error.
     *
     * <p>Replace this with your actual parse failure assertion utility if you have one.</p>
     */
    private void assertParseError(String sql) {
        var result = parseExpr(sql);
        assertFalse(result.ok());
    }
}
