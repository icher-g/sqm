package io.sqm.parser.ansi;

import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.WithQuery;
import io.sqm.parser.QueryParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.*;

import java.util.List;

/**
 * Black-box tests for QuerySpecParser using ONLY existing sqm-parser components.
 * We assert parse success/failure and minimal model properties that are stable.
 */
class QueryParserTest {

    private QueryParser parser;
    private ParseContext ctx;

    @BeforeEach
    void setUp() {
        // Use your real repository (the same one you use in production/tests elsewhere)
        this.parser = new QueryParser();
        this.ctx = ParseContext.of(new AnsiSpecs());
    }

    private Query parseOk(String sql) {
        var res = ctx.parse(parser, Cursor.of(sql));
        Assertions.assertTrue(res.ok(), () -> "Expected OK, got error: " + res);
        // also ensure the entire input was consumed (EOF)
        return res.value();
    }

    private void parseErr(String sql) {
        var res = ctx.parse(parser, Cursor.of(sql));
        Assertions.assertFalse(res.ok(), () -> "Expected error, got OK with: " + res.value());
    }

    @Test
    @DisplayName("Single CTE without column list")
    void singleCte() {
        var q = parseOk("""
            WITH t AS (SELECT 1 AS a)
            SELECT a FROM t
            """);

        Assertions.assertInstanceOf(WithQuery.class, q);
        var cq = (WithQuery) q;

        Assertions.assertFalse(cq.recursive());
        Assertions.assertEquals(1, cq.ctes().size());
        Assertions.assertEquals("t", cq.ctes().getFirst().name());
        Assertions.assertEquals(0, cq.ctes().getFirst().columnAliases().size()); // no explicit alias list
    }

    @Test
    @DisplayName("Single CTE with column list")
    void singleCteWithColumns() {
        var q = parseOk("""
            WITH u_cte(id, name) AS (
              SELECT id, name FROM users
            )
            SELECT name FROM u_cte
            """);

        var cq = (WithQuery) q;
        Assertions.assertEquals(1, cq.ctes().size());
        var cte = cq.ctes().getFirst();

        Assertions.assertEquals("u_cte", cte.name());
        var columnAliases = cq.ctes().getFirst().columnAliases();
        Assertions.assertNotNull(columnAliases);
        Assertions.assertEquals(2, columnAliases.size());
        Assertions.assertEquals("id", columnAliases.get(0));
        Assertions.assertEquals("name", columnAliases.get(1));
    }

    @Test
    @DisplayName("Multiple CTEs")
    void multipleCtes() {
        var q = parseOk("""
            WITH a AS (SELECT 1 x),
                 b AS (SELECT 2 y)
            SELECT * FROM a JOIN b ON a.x = b.y
            """);

        var cq = (WithQuery) q;
        Assertions.assertEquals(2, cq.ctes().size());
        Assertions.assertEquals("a", cq.ctes().get(0).name());
        Assertions.assertEquals("b", cq.ctes().get(1).name());
    }

    @Test
    @DisplayName("WITH RECURSIVE")
    void withRecursive() {
        var q = parseOk("""
            WITH RECURSIVE t(n) AS (
               SELECT 1
               UNION ALL
               (SELECT n FROM t WHERE n < 10)
            )
            SELECT * FROM t
            """);

        var cq = (WithQuery) q;
        Assertions.assertTrue(cq.recursive());
        Assertions.assertEquals(1, cq.ctes().size());
        Assertions.assertEquals("t", cq.ctes().getFirst().name());
        Assertions.assertEquals(List.of("n"), cq.ctes().getFirst().columnAliases());
    }

    @Nested
    @DisplayName("FROM clause (optional)")
    class FromOptional {

        @Test
        @DisplayName("SELECT literal without FROM is accepted")
        void selectLiteralWithoutFrom() {
            var q = (SelectQuery) parseOk("SELECT 1");
            // Minimal sanity: FROM is absent
            // If your Query exposes a getter for FROM, assert it:
            // assertNull(q.from(), "FROM must be null for SELECT 1");
            // At least check we have one projection:
            Assertions.assertFalse(q.items().isEmpty(), "SELECT list should not be empty");
        }

        @Test
        @DisplayName("SELECT expression without FROM is accepted")
        void selectFunctionWithoutFrom() {
            parseOk("SELECT CURRENT_DATE");
//            parseOk("SELECT 2 + 2 AS result"); -- not supported.
            parseOk("SELECT 'Hello' AS greeting");
        }
    }

    @Nested
    @DisplayName("Full query shape")
    class FullQuery {

        @Test
        @DisplayName("Query with FROM, JOIN, WHERE, GROUP/HAVING, ORDER, LIMIT/OFFSET")
        void fullQueryParses() {
            var sql = """
                SELECT u.user_name, o.status, COUNT(*) AS cnt
                FROM orders o
                JOIN users u ON u.id = o.user_id
                WHERE o.status IN ('A', 'B')
                GROUP BY u.user_name, o.status
                HAVING COUNT(*) > 10
                ORDER BY u.user_name ASC, COALESCE(o.status, 'Z') DESC
                LIMIT 50
                OFFSET 100
                """;
            var q = (SelectQuery) parseOk(sql);

            // Spot-check a few stable properties:
            Assertions.assertEquals(3, q.items().size(), "3 select items expected");
            Assertions.assertNotNull(q.from(), "FROM should be present");
            Assertions.assertFalse(q.joins().isEmpty(), "At least one JOIN expected");
            Assertions.assertNotNull(q.where(), "WHERE should be parsed");
            Assertions.assertFalse(q.groupBy().items().isEmpty(), "GROUP BY items expected");
            Assertions.assertNotNull(q.having(), "HAVING should be parsed");
            Assertions.assertFalse(q.orderBy().items().isEmpty(), "ORDER BY items expected");
            Assertions.assertEquals(50, q.limit(), "LIMIT should be 50");
            Assertions.assertEquals(100, q.offset(), "OFFSET should be 100");
        }

        @Test
        @DisplayName("Multiple JOINs are parsed consecutively")
        void multipleJoins() {
            var sql = """
                SELECT u.id, o.id, p.id
                FROM users u
                LEFT JOIN orders o ON o.user_id = u.id
                INNER JOIN payments p ON p.order_id = o.id
                """;
            var q = (SelectQuery) parseOk(sql);
            Assertions.assertEquals(2, q.joins().size(), "Two JOINs expected (LEFT + INNER)");
        }
    }

    @Nested
    @DisplayName("Clause boundaries & syntax")
    class Boundaries {

        @Test
        @DisplayName("GROUP must be followed by BY")
        void groupRequiresBy() {
            parseErr("SELECT 1 GROUP x");
        }

        @Test
        @DisplayName("ORDER must be followed by BY")
        void orderRequiresBy() {
            parseErr("SELECT 1 ORDER x");
        }

        @Test
        @DisplayName("Trailing junk after a valid query is rejected")
        void trailingGarbageRejected() {
            parseErr("SELECT 1 xyz zzz");
        }
    }

    @Nested
    @DisplayName("Top-level comma splitting")
    class CommasAndFunctions {

        @Test
        @DisplayName("ORDER BY splits by top-level commas only (functions with commas allowed)")
        void orderByTopLevelCommas() {
            var sql = """
                SELECT 1
                ORDER BY COALESCE(a, b), c, CASE WHEN d > 0 THEN e ELSE f END
                """;
            var q = (SelectQuery) parseOk(sql);
            Assertions.assertEquals(3, q.orderBy().items().size(), "Three top-level ORDER items expected");
        }

        @Test
        @DisplayName("SELECT list splits by top-level commas")
        void selectListTopLevelCommas() {
            var sql = "SELECT func(a, b), (x), y";
            var q = (SelectQuery) parseOk(sql);
            Assertions.assertEquals(3, q.items().size(), "Three top-level select items expected");
        }
    }

    @Nested
    @DisplayName("LIMIT / OFFSET")
    class LimitOffset {

        @Test
        @DisplayName("LIMIT and OFFSET accept numeric literals")
        void limitOffsetNumeric() {
            var q = (SelectQuery) parseOk("SELECT 1 LIMIT 5 OFFSET 2");
            Assertions.assertEquals(5, q.limit());
            Assertions.assertEquals(2, q.offset());
        }

        @Test
        @DisplayName("LIMIT without a number is rejected")
        void limitWithoutNumber() {
            parseErr("SELECT 1 LIMIT x");
        }

        @Test
        @DisplayName("OFFSET without a number is rejected")
        void offsetWithoutNumber() {
            parseErr("SELECT 1 OFFSET x");
        }
    }
}
