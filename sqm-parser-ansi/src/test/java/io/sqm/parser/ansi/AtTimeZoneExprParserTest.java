package io.sqm.parser.ansi;

import io.sqm.core.AtTimeZoneExpr;
import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for {@link AtTimeZoneExpr}.
 *
 * <p>AT TIME ZONE is a PostgreSQL-specific extension to ANSI SQL and is NOT
 * supported by the ANSI parser. Since AT, TIME, and ZONE are not keywords in
 * the ANSI lexer, expressions containing AT TIME ZONE will either:
 * <ul>
 *   <li>Parse as incomplete (if AT TIME ZONE appears after a valid expression)</li>
 *   <li>Parse AT as a column reference (if AT appears alone)</li>
 *   <li>Fail with a parse error (if AT TIME ZONE appears in a context that requires a complete expression)</li>
 * </ul>
 *
 * <p>The AtTimeZoneExprParser is implemented with a `match()` method that returns false,
 * meaning it will never participate in infix parsing. This is correct since AT, TIME, ZONE
 * are not recognized keywords.</p>
 */
class AtTimeZoneExprParserTest {

    /**
     * Tests that AT can be parsed as a column reference (identifier).
     * Since AT is not a keyword, it's a valid identifier.
     */
    @Test
    void atCanBeParsedAsColumnReference() {
        var result = parseExpr("AT");
        assertTrue(result.ok(), "AT should parse as a column reference");
        assertInstanceOf(ColumnExpr.class, result.value(), "Should not parse as AT TIME ZONE");
    }

    /**
     * Tests that TIME can be parsed as a column reference (identifier).
     * Since TIME is not a keyword in ANSI lexer, it's valid identifier.
     */
    @Test
    void timeCanBeParsedAsColumnReference() {
        var result = parseExpr("TIME");
        assertTrue(result.ok(), "TIME should parse as a column reference");
    }

    /**
     * Tests that parsing fails when AT TIME ZONE appears in a position
     * that requires a complete expression.
     * The parser will fail because it cannot make sense of the token sequence.
     */
    @Test
    void failsWhenAtTimeZoneFollowsValidExpression() {
        // When AT TIME ZONE appears after now(), the parser successfully parses now()
        // but then fails because AT TIME ZONE doesn't make sense after an expression.
        var result = parseExpr("now() AT TIME ZONE 'UTC'");
        assertFalse(result.ok(),
            "Should fail: AT TIME ZONE is not recognized by ANSI parser");
    }

    /**
     * Tests that AT TIME ZONE expressions cannot be created through the
     * parser - they're only available through DSL construction for testing purposes.
     */
    @Test
    void atTimeZoneNotRecognizedAsInfixOperator() {
        // Even if we had a valid expression before AT TIME ZONE, the parser
        // won't recognize AT as starting an infix operator since it's not a keyword.
        var result = parseExpr("col() AT TIME ZONE 'UTC'");
        assertFalse(result.ok(),
            "AT TIME ZONE is not recognized as an infix operator");
    }

    /**
     * Tests parsing with ZONE as a column reference (identifier).
     */
    @Test
    void zoneCanBeParsedAsColumnReference() {
        var result = parseExpr("ZONE");
        assertTrue(result.ok(), "ZONE should parse as a column reference");
    }

    /**
     * Tests parsing multiple AT expressions as column references.
     */
    @Test
    void multipleAtTokensAsColumnReferences() {
        var result = parseExpr("AT");
        assertTrue(result.ok(), "AT should parse as column reference");
        assertInstanceOf(ColumnExpr.class, result.value());
    }

    /**
     * Tests parsing function call without AT TIME ZONE.
     */
    @Test
    void functionCallWithoutAtTimeZone() {
        var result = parseExpr("now()");
        assertTrue(result.ok(), "Function calls should parse normally");
    }

    /**
     * Tests with timezone string literal alone.
     */
    @Test
    void timezoneStringLiteralAlone() {
        var result = parseExpr("'UTC'");
        assertTrue(result.ok(), "String literals should parse successfully");
    }

    /**
     * Tests column with qualified name does not confuse parser.
     */
    @Test
    void qualifiedColumnExpression() {
        var result = parseExpr("t.created_at");
        assertTrue(result.ok(), "Qualified columns should parse normally");
    }

    /**
     * Helper method to parse an expression using the ANSI parser.
     */
    private ParseResult<? extends Expression> parseExpr(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        return ctx.parse(Expression.class, sql);
    }
}
