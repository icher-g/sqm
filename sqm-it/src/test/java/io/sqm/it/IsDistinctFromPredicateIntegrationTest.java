package io.sqm.it;

import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link io.sqm.core.IsDistinctFromPredicate} round-trip:
 * SQL → Parse → Model → Render → SQL
 */
class IsDistinctFromPredicateIntegrationTest {

    private ParseContext parseContext;
    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        parseContext = ParseContext.of(new AnsiSpecs());
        renderContext = RenderContext.of(new AnsiDialect());
    }

    /**
     * Normalizes whitespace in SQL strings for comparison.
     * Replaces multiple spaces, tabs, and newlines with a single space.
     */
    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void testRoundTripIsDistinctFrom() {
        String originalSql = "a IS DISTINCT FROM b";

        var parseResult = parseContext.parse(io.sqm.core.IsDistinctFromPredicate.class, originalSql);
        assertTrue(parseResult.ok());

        var predicate = parseResult.value();
        var renderedSql = renderContext.render(predicate).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripIsNotDistinctFrom() {
        String originalSql = "x IS NOT DISTINCT FROM y";

        var parseResult = parseContext.parse(io.sqm.core.IsDistinctFromPredicate.class, originalSql);
        assertTrue(parseResult.ok());

        var predicate = parseResult.value();
        var renderedSql = renderContext.render(predicate).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripWithLiteral() {
        String originalSql = "status IS DISTINCT FROM 'active'";

        var parseResult = parseContext.parse(io.sqm.core.IsDistinctFromPredicate.class, originalSql);
        assertTrue(parseResult.ok());

        var predicate = parseResult.value();
        var renderedSql = renderContext.render(predicate).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripWithNull() {
        String originalSql = "value IS DISTINCT FROM NULL";

        var parseResult = parseContext.parse(io.sqm.core.IsDistinctFromPredicate.class, originalSql);
        assertTrue(parseResult.ok());

        var predicate = parseResult.value();
        var renderedSql = renderContext.render(predicate).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripWithNumericLiteral() {
        String originalSql = "count IS NOT DISTINCT FROM 42";

        var parseResult = parseContext.parse(io.sqm.core.IsDistinctFromPredicate.class, originalSql);
        assertTrue(parseResult.ok());

        var predicate = parseResult.value();
        var renderedSql = renderContext.render(predicate).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripWithQualifiedColumns() {
        String originalSql = "t1.id IS DISTINCT FROM t2.id";

        var parseResult = parseContext.parse(io.sqm.core.IsDistinctFromPredicate.class, originalSql);
        assertTrue(parseResult.ok());

        var predicate = parseResult.value();
        var renderedSql = renderContext.render(predicate).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripInSelectQuery() {
        String originalSql = "SELECT id, name FROM users WHERE status IS DISTINCT FROM 'deleted'";

        var parseResult = parseContext.parse(io.sqm.core.Query.class, originalSql);
        assertTrue(parseResult.ok());

        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripInJoinCondition() {
        String originalSql = "SELECT u.name FROM users AS u INNER JOIN orders AS o ON u.id IS NOT DISTINCT FROM o.user_id";

        var parseResult = parseContext.parse(io.sqm.core.Query.class, originalSql);
        assertTrue(parseResult.ok());

        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripWithComplexExpression() {
        String originalSql = "a + 1 IS DISTINCT FROM b * 2";

        var parseResult = parseContext.parse(io.sqm.core.IsDistinctFromPredicate.class, originalSql);
        assertTrue(parseResult.ok());

        var predicate = parseResult.value();
        var renderedSql = renderContext.render(predicate).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripWithFunctionCall() {
        String originalSql = "UPPER(name) IS NOT DISTINCT FROM 'JOHN'";

        var parseResult = parseContext.parse(io.sqm.core.IsDistinctFromPredicate.class, originalSql);
        assertTrue(parseResult.ok());

        var predicate = parseResult.value();
        var renderedSql = renderContext.render(predicate).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripInCompositePredicate() {
        String originalSql = "SELECT * FROM t WHERE a IS DISTINCT FROM b AND c = d";

        var parseResult = parseContext.parse(io.sqm.core.Query.class, originalSql);
        assertTrue(parseResult.ok());

        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripInOrPredicate() {
        String originalSql = "SELECT * FROM t WHERE x IS NOT DISTINCT FROM NULL OR y IS NULL";

        var parseResult = parseContext.parse(io.sqm.core.Query.class, originalSql);
        assertTrue(parseResult.ok());

        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripInHavingClause() {
        String originalSql = "SELECT dept, COUNT(*) AS cnt FROM employees GROUP BY dept HAVING COUNT(*) IS DISTINCT FROM 10";

        var parseResult = parseContext.parse(io.sqm.core.Query.class, originalSql);
        assertTrue(parseResult.ok());

        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripMultiplePredicates() {
        String originalSql = "SELECT * FROM t WHERE a IS DISTINCT FROM b AND c IS NOT DISTINCT FROM d";

        var parseResult = parseContext.parse(io.sqm.core.Query.class, originalSql);
        assertTrue(parseResult.ok());

        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripWithSubquery() {
        String originalSql = "SELECT * FROM t WHERE id IS DISTINCT FROM ( SELECT max_id FROM limits )";

        var parseResult = parseContext.parse(io.sqm.core.Query.class, originalSql);
        assertTrue(parseResult.ok());

        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    void testRoundTripCaseInsensitive() {
        String inputSql = "a is distinct from b";
        String expectedSql = "a IS DISTINCT FROM b";

        var parseResult = parseContext.parse(io.sqm.core.IsDistinctFromPredicate.class, inputSql);
        assertTrue(parseResult.ok());

        var predicate = parseResult.value();
        var renderedSql = renderContext.render(predicate).sql();

        assertEquals(expectedSql, renderedSql);
    }

    @Test
    void testDslToSql() {
        var predicate = io.sqm.core.IsDistinctFromPredicate.of(
            col("a"),
            lit(10),
            false
        );

        var sql = renderContext.render(predicate).sql();

        assertEquals("a IS DISTINCT FROM 10", sql);

        // Now parse it back
        var parseResult = parseContext.parse(io.sqm.core.IsDistinctFromPredicate.class, sql);
        assertTrue(parseResult.ok());

        var reparsed = parseResult.value();
        var reserialized = renderContext.render(reparsed).sql();

        assertEquals(sql, reserialized);
    }

    @Test
    void testComplexQueryRoundTrip() {
        String originalSql = """
            SELECT u.id, u.name, o.total
            FROM users AS u
            INNER JOIN orders AS o ON u.id IS NOT DISTINCT FROM o.user_id
            WHERE u.status IS DISTINCT FROM 'deleted'
            AND o.total IS NOT DISTINCT FROM 100""".replaceAll("\\R", " ").replaceAll("\\s+", " ").trim();

        var parseResult = parseContext.parse(io.sqm.core.Query.class, originalSql);
        assertTrue(parseResult.ok());

        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();

        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }
}
