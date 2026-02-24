package io.sqm.parser.postgresql;

import io.sqm.core.*;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for PostgreSQL {@link RegexPredicate}.
 *
 * <p>Tests regex matching operators:</p>
 * <ul>
 *   <li>{@code ~} - case-sensitive match</li>
 *   <li>{@code ~*} - case-insensitive match</li>
 *   <li>{@code !~} - negated case-sensitive match</li>
 *   <li>{@code !~*} - negated case-insensitive match</li>
 * </ul>
 */
@DisplayName("PostgreSQL RegexPredicateParser Tests")
class RegexPredicateParserTest {

    private final ParseContext parseContext = ParseContext.of(new PostgresSpecs());

    @Test
    @DisplayName("Parse case-sensitive regex match")
    void parsesCaseSensitiveMatch() {
        var result = parsePredicate("name ~ '^A.*'");

        assertTrue(result.ok());
        var pred = assertInstanceOf(RegexPredicate.class, result.value());

        assertEquals(RegexMode.MATCH, pred.mode());
        assertFalse(pred.negated());
        assertInstanceOf(ColumnExpr.class, pred.value());
        assertInstanceOf(LiteralExpr.class, pred.pattern());
    }

    @Test
    @DisplayName("Parse case-insensitive regex match")
    void parsesCaseInsensitiveMatch() {
        var result = parsePredicate("email ~* '@example\\.com$'");

        assertTrue(result.ok());
        var pred = assertInstanceOf(RegexPredicate.class, result.value());

        assertEquals(RegexMode.MATCH_INSENSITIVE, pred.mode());
        assertFalse(pred.negated());
    }

    @Test
    @DisplayName("Parse negated case-sensitive regex match")
    void parsesNegatedCaseSensitiveMatch() {
        var result = parsePredicate("name !~ '^test'");

        assertTrue(result.ok());
        var pred = assertInstanceOf(RegexPredicate.class, result.value());

        assertEquals(RegexMode.MATCH, pred.mode());
        assertTrue(pred.negated());
    }

    @Test
    @DisplayName("Parse negated case-insensitive regex match")
    void parsesNegatedCaseInsensitiveMatch() {
        var result = parsePredicate("email !~* '@spam\\.com$'");

        assertTrue(result.ok());
        var pred = assertInstanceOf(RegexPredicate.class, result.value());

        assertEquals(RegexMode.MATCH_INSENSITIVE, pred.mode());
        assertTrue(pred.negated());
    }

    @Test
    @DisplayName("Parse regex predicate in WHERE clause")
    void parsesRegexInWhereClause() {
        var qResult = parseQuery("SELECT * FROM users WHERE email ~ '@company\\.com$'");

        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        assertNotNull(query.where());

        assertInstanceOf(RegexPredicate.class, query.where());
    }

    @Test
    @DisplayName("Parse regex with column pattern")
    void parsesRegexWithColumnPattern() {
        var result = parsePredicate("name ~ pattern_col");

        assertTrue(result.ok());
        var pred = assertInstanceOf(RegexPredicate.class, result.value());

        assertInstanceOf(ColumnExpr.class, pred.value());
        assertInstanceOf(ColumnExpr.class, pred.pattern());
    }

    @Test
    @DisplayName("Parse regex combined with AND")
    void parsesRegexCombinedWithAnd() {
        var result = parsePredicate("name ~ '^A' AND email ~ '@example\\.com$'");

        assertTrue(result.ok());
        var pred = assertInstanceOf(AndPredicate.class, result.value());

        assertInstanceOf(RegexPredicate.class, pred.lhs());
        assertInstanceOf(RegexPredicate.class, pred.rhs());
    }

    @Test
    @DisplayName("Parse regex combined with OR")
    void parsesRegexCombinedWithOr() {
        var result = parsePredicate("name ~* 'test' OR name ~* 'demo'");

        assertTrue(result.ok());
        var pred = assertInstanceOf(OrPredicate.class, result.value());

        assertInstanceOf(RegexPredicate.class, pred.lhs());
        assertInstanceOf(RegexPredicate.class, pred.rhs());
    }

    @Test
    @DisplayName("Parse regex on qualified column")
    void parsesRegexOnQualifiedColumn() {
        var result = parsePredicate("t.name ~ '^A.*'");

        assertTrue(result.ok());
        var pred = assertInstanceOf(RegexPredicate.class, result.value());

        var col = assertInstanceOf(ColumnExpr.class, pred.value());
        assertEquals("t", col.tableAlias().value());
        assertEquals("name", col.name().value());
    }

    @Test
    @DisplayName("Parse regex with function call as value")
    void parsesRegexWithFunctionValue() {
        var result = parsePredicate("UPPER(name) ~ '^A.*'");

        assertTrue(result.ok());
        var pred = assertInstanceOf(RegexPredicate.class, result.value());

        assertInstanceOf(FunctionExpr.class, pred.value());
    }

    @Test
    @DisplayName("Reject regex predicate without right-hand expression")
    void rejectsRegexWithoutRightExpression() {
        assertParseError("name ~");
    }

    @Test
    @DisplayName("Reject regex predicate with missing pattern after operator")
    void rejectsRegexMissingPattern() {
        assertParseError("name !~*");
    }

    private ParseResult<? extends Predicate> parsePredicate(String sql) {
        return parseContext.parse(Predicate.class, sql);
    }

    private ParseResult<? extends Query> parseQuery(String sql) {
        return parseContext.parse(Query.class, sql);
    }

    private void assertParseError(String sql) {
        var result = parsePredicate(sql);
        if (result.ok()) {
            fail("Expected parse error for: " + sql);
        }
    }
}

