package io.sqm.parser.ansi;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.StatementSequence;
import io.sqm.core.WithQuery;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.*;

class CompositeQueryParserTest {
    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());

    @Test
    void unpaginatedChainsEndAtEofOrSemicolon() {
        for (var sql : new String[]{"SELECT 1 UNION SELECT 2", "SELECT 1 UNION SELECT 2;",
            "SELECT 1 UNION SELECT 2 EXCEPT SELECT 3"}) {
            var query = assertInstanceOf(CompositeQuery.class, parse(sql));
            assertNull(query.orderBy());
            assertNull(query.limitOffset().limit());
            query.terms().forEach(term -> assertNoTail((SelectQuery) term));
        }
    }

    @Test
    void lookaheadDoesNotCrossStatementBoundaries() {
        var result = ctx.parse(StatementSequence.class,
            "SELECT 1 ORDER BY 1 LIMIT 3; SELECT 2 UNION SELECT 3 ORDER BY 1 LIMIT 5;");
        assertTrue(result.ok(), result::errorMessage);
        var first = assertInstanceOf(SelectQuery.class, result.value().statements().getFirst());
        assertNotNull(first.orderBy());
        assertEquals(lit(3L), first.limitOffset().limit());
        var last = assertInstanceOf(CompositeQuery.class, result.value().statements().getLast());
        assertEquals(lit(5L), last.limitOffset().limit());
    }

    @Test
    void trailingClausesBelongToTheSetResult() {
        for (var op : new String[]{"UNION", "UNION ALL", "INTERSECT", "INTERSECT ALL", "EXCEPT", "EXCEPT ALL"}) {
            for (var tail : new String[]{"ORDER BY id", "LIMIT 5", "OFFSET 2", "LIMIT 5 OFFSET 2",
                "ORDER BY id DESC LIMIT 5 OFFSET 2", "OFFSET 2 ROWS FETCH NEXT 5 ROWS ONLY", "FETCH FIRST 5 ROWS ONLY"}) {
                var query = assertInstanceOf(CompositeQuery.class,
                    parse("SELECT id FROM users " + op + " SELECT id FROM orders " + tail));
                assertEquals(tail.startsWith("ORDER"), query.orderBy() != null);
                assertEquals(tail.contains("5"), query.limitOffset().limit() != null);
                assertEquals(tail.contains("2"), query.limitOffset().offset() != null);
                query.terms().forEach(term -> assertNoTail(assertInstanceOf(SelectQuery.class, term)));
            }
        }
    }

    @Test
    void parenthesesPreserveLocalAndGlobalTails() {
        var query = assertInstanceOf(CompositeQuery.class, parse("""
            (SELECT id FROM users ORDER BY id LIMIT 3)
            UNION (SELECT id FROM orders ORDER BY id DESC LIMIT 4 OFFSET 1)
            ORDER BY id LIMIT 5 OFFSET 2
            """));
        assertEquals(lit(3L), ((SelectQuery) query.terms().getFirst()).limitOffset().limit());
        assertEquals(lit(4L), ((SelectQuery) query.terms().getLast()).limitOffset().limit());
        assertEquals(lit(5L), query.limitOffset().limit());
        assertEquals(lit(2L), query.limitOffset().offset());
        assertNotNull(query.orderBy());

        var localOnly = assertInstanceOf(CompositeQuery.class, parse(
            "SELECT id FROM users UNION (SELECT id FROM orders ORDER BY id LIMIT 4)"));
        assertNull(localOnly.orderBy());
        assertNull(localOnly.limitOffset().limit());
        assertNotNull(((SelectQuery) localOnly.terms().getLast()).orderBy());
    }

    @Test
    void nestedSetExpressionsAndWithBodiesKeepTheirScope() {
        var with = assertInstanceOf(WithQuery.class, parse("""
            WITH x AS (SELECT id FROM users UNION SELECT id FROM orders ORDER BY id LIMIT 3)
            SELECT id FROM x EXCEPT
            (SELECT id FROM users INTERSECT SELECT id FROM orders ORDER BY id LIMIT 4)
            ORDER BY id LIMIT 5;
            """));
        assertEquals(lit(3L), ((CompositeQuery) with.ctes().getFirst().body()).limitOffset().limit());
        var body = assertInstanceOf(CompositeQuery.class, with.body());
        assertEquals(lit(5L), body.limitOffset().limit());
        assertNoTail((SelectQuery) body.terms().getFirst());
        var nested = assertInstanceOf(CompositeQuery.class, body.terms().getLast());
        assertEquals(lit(4L), nested.limitOffset().limit());
        nested.terms().forEach(term -> assertNoTail((SelectQuery) term));
    }

    @Test
    void standaloneAndNestedSelectsStillOwnTheirTails() {
        var query = assertInstanceOf(SelectQuery.class, parse(
            "SELECT (SELECT id FROM orders ORDER BY id LIMIT 1) FROM users ORDER BY 1 LIMIT 5"));
        assertNotNull(query.orderBy());
        assertEquals(lit(5L), query.limitOffset().limit());
        var composite = assertInstanceOf(CompositeQuery.class, parse(
            "SELECT (SELECT id FROM orders ORDER BY id LIMIT 1) UNION SELECT 2 ORDER BY 1 LIMIT 5"));
        assertEquals(lit(5L), composite.limitOffset().limit());
    }

    @Test
    void malformedTermsAndTailsAreRejected() {
        for (var sql : new String[]{"SELECT 1 UNION", "SELECT 1 UNION SELECT FROM orders",
            "SELECT 1 UNION SELECT 2 ORDER BY", "SELECT 1 UNION SELECT 2 LIMIT",
            "SELECT 1 ORDER BY 1 UNION SELECT 2", "SELECT 1 UNION SELECT 2 unexpected extra",
            "SELECT 1 UNION SELECT 2 LIMIT 3 UNION SELECT 4"}) {
            assertFalse(ctx.parse(Query.class, sql).ok(), sql);
        }
    }

    private Query parse(String sql) {
        var result = ctx.parse(Query.class, sql);
        assertTrue(result.ok(), result::errorMessage);
        return result.value();
    }

    private void assertNoTail(SelectQuery query) {
        assertNull(query.orderBy());
        assertTrue(query.limitOffset() == null ||
            (query.limitOffset().limit() == null && query.limitOffset().offset() == null));
    }
}
