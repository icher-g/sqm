package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for full query parsing covering various SQL constructs.
 * This increases coverage for QueryParser, AtomicQueryParser, and related base parsers.
 */
class ComprehensiveQueryParserTest {

    private ParseContext ctx;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
    }

    @Test
    void parsesSimpleSelect() {
        var result = ctx.parse(Query.class, "SELECT id, name FROM users");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
    }

    @Test
    void parsesSelectWithWhere() {
        var result = ctx.parse(Query.class, "SELECT id, name FROM users WHERE active = TRUE");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertNotNull(select.where());
    }

    @Test
    void parsesSelectWithInnerJoin() {
        var result = ctx.parse(Query.class, 
            "SELECT u.id, u.name, o.total FROM users u INNER JOIN orders o ON u.id = o.user_id");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertEquals(1, select.joins().size());
        assertInstanceOf(OnJoin.class, select.joins().getFirst());
    }

    @Test
    void parsesSelectWithLeftJoin() {
        var result = ctx.parse(Query.class, 
            "SELECT u.id, u.name FROM users u LEFT JOIN orders o ON u.id = o.user_id");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertEquals(1, select.joins().size());
        OnJoin join = (OnJoin) select.joins().getFirst();
        assertEquals(JoinKind.LEFT, join.kind());
    }

    @Test
    void parsesSelectWithRightJoin() {
        var result = ctx.parse(Query.class, 
            "SELECT * FROM orders o RIGHT JOIN users u ON o.user_id = u.id");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        OnJoin join = (OnJoin) select.joins().getFirst();
        assertEquals(JoinKind.RIGHT, join.kind());
    }

    @Test
    void parsesSelectWithFullJoin() {
        var result = ctx.parse(Query.class, 
            "SELECT * FROM table1 t1 FULL JOIN table2 t2 ON t1.id = t2.id");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        OnJoin join = (OnJoin) select.joins().getFirst();
        assertEquals(JoinKind.FULL, join.kind());
    }

    @Test
    void parsesSelectWithCrossJoin() {
        var result = ctx.parse(Query.class, "SELECT * FROM table1 CROSS JOIN table2");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertInstanceOf(CrossJoin.class, select.joins().getFirst());
    }

    @Test
    void parsesSelectWithNaturalJoin() {
        var result = ctx.parse(Query.class, "SELECT * FROM table1 NATURAL JOIN table2");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertInstanceOf(NaturalJoin.class, select.joins().getFirst());
    }

     @Test
     void parsesSelectWithUsingJoin() {
         var result = ctx.parse(Query.class,
             "SELECT * FROM users u JOIN orders o USING (user_id)");
         assertTrue(result.ok());
         var query = result.value();
         assertInstanceOf(SelectQuery.class, query);
         SelectQuery select = (SelectQuery) query;
         assertInstanceOf(UsingJoin.class, select.joins().getFirst());
     }

    @Test
    void parsesSelectWithMultipleJoins() {
        var result = ctx.parse(Query.class, 
            "SELECT * FROM users u " +
            "JOIN orders o ON u.id = o.user_id " +
            "JOIN products p ON o.product_id = p.id");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertEquals(2, select.joins().size());
    }

    @Test
    void parsesSelectWithGroupBy() {
        var result = ctx.parse(Query.class, 
            "SELECT category, COUNT(*) FROM products GROUP BY category");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertNotNull(select.groupBy());
    }

    @Test
    void parsesSelectWithHaving() {
        var result = ctx.parse(Query.class, 
            "SELECT category, COUNT(*) AS cnt FROM products GROUP BY category HAVING COUNT(*) > 10");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertNotNull(select.groupBy());
        assertNotNull(select.having());
    }

    @Test
    void parsesSelectWithOrderBy() {
        var result = ctx.parse(Query.class, 
            "SELECT name, age FROM users ORDER BY age DESC, name ASC");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertNotNull(select.orderBy());
        assertEquals(2, select.orderBy().items().size());
    }

    @Test
    void parsesSelectWithLimit() {
        var result = ctx.parse(Query.class, "SELECT * FROM users LIMIT 10");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertNotNull(select.limitOffset().limit());
    }

    @Test
    void parsesSelectWithOffset() {
        var result = ctx.parse(Query.class, "SELECT * FROM users OFFSET 20");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertNotNull(select.limitOffset().offset());
    }

    @Test
    void parsesSelectWithLimitAndOffset() {
        var result = ctx.parse(Query.class, "SELECT * FROM users LIMIT 10 OFFSET 20");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertNotNull(select.limitOffset().limit());
        assertNotNull(select.limitOffset().offset());
    }

    @Test
    void parsesSelectWithDistinct() {
        var result = ctx.parse(Query.class, "SELECT DISTINCT category FROM products");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertNotNull(select.distinct());
    }

    @Test
    void parsesUnionQuery() {
        var result = ctx.parse(Query.class, 
            "SELECT id FROM table1 UNION SELECT id FROM table2");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(CompositeQuery.class, query);
        CompositeQuery composite = (CompositeQuery) query;
        assertEquals(2, composite.terms().size());
        assertEquals(SetOperator.UNION, composite.ops().getFirst());
    }

    @Test
    void parsesUnionAllQuery() {
        var result = ctx.parse(Query.class, 
            "SELECT id FROM table1 UNION ALL SELECT id FROM table2");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(CompositeQuery.class, query);
        CompositeQuery composite = (CompositeQuery) query;
        assertEquals(SetOperator.UNION_ALL, composite.ops().getFirst());
    }

    @Test
    void parsesIntersectQuery() {
        var result = ctx.parse(Query.class, 
            "SELECT id FROM table1 INTERSECT SELECT id FROM table2");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(CompositeQuery.class, query);
        CompositeQuery composite = (CompositeQuery) query;
        assertEquals(SetOperator.INTERSECT, composite.ops().getFirst());
    }

    @Test
    void parsesExceptQuery() {
        var result = ctx.parse(Query.class, 
            "SELECT id FROM table1 EXCEPT SELECT id FROM table2");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(CompositeQuery.class, query);
        CompositeQuery composite = (CompositeQuery) query;
        assertEquals(SetOperator.EXCEPT, composite.ops().getFirst());
    }

    @Test
    void parsesWithQuery() {
        var result = ctx.parse(Query.class, 
            "WITH active_users AS (SELECT * FROM users WHERE active = TRUE) " +
            "SELECT * FROM active_users");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(WithQuery.class, query);
        WithQuery with = (WithQuery) query;
        assertEquals(1, with.ctes().size());
        assertNotNull(with.body());
    }

    @Test
    void parsesWithMultipleCTEs() {
        var result = ctx.parse(Query.class, 
            "WITH " +
            "cte1 AS (SELECT * FROM table1), " +
            "cte2 AS (SELECT * FROM table2) " +
            "SELECT * FROM cte1 JOIN cte2 ON cte1.id = cte2.id");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(WithQuery.class, query);
        WithQuery with = (WithQuery) query;
        assertEquals(2, with.ctes().size());
    }

    @Test
    void parsesWithRecursive() {
        var result = ctx.parse(Query.class, 
            "WITH RECURSIVE numbers AS (" +
            "  SELECT 1 AS n " +
            "  UNION ALL " +
            "  SELECT n + 1 FROM numbers WHERE n < 10" +
            ") SELECT * FROM numbers");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(WithQuery.class, query);
        WithQuery with = (WithQuery) query;
        assertTrue(with.recursive());
    }

    @Test
    void parsesSubqueryInFrom() {
        var result = ctx.parse(Query.class, 
            "SELECT * FROM (SELECT id, name FROM users) AS u");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertInstanceOf(QueryTable.class, select.from());
    }

     @Test
     void parsesValuesInFrom() {
         var result = ctx.parse(Query.class,
             "SELECT * FROM (VALUES (1, 'a'), (2, 'b')) AS t(id, name)");
         assertTrue(result.ok());
         var query = result.value();
         assertInstanceOf(SelectQuery.class, query);
         SelectQuery select = (SelectQuery) query;
         assertInstanceOf(ValuesTable.class, select.from());
     }

    @Test
    void parsesSelectWithoutFrom() {
        var result = ctx.parse(Query.class, "SELECT 1, 2, 3");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertNull(select.from());
    }

    @Test
    void parsesSelectStarFromMultipleTables() {
        var result = ctx.parse(Query.class, 
            "SELECT u.*, o.* FROM users u JOIN orders o ON u.id = o.user_id");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertEquals(2, select.items().size());
    }

    @Test
    void parsesComplexNestedQuery() {
        var result = ctx.parse(Query.class, 
            "SELECT * FROM users WHERE id IN (" +
            "  SELECT user_id FROM orders WHERE total > 1000" +
            ")");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
    }

    @Test
    void parsesQueryWithWindowFunction() {
        var result = ctx.parse(Query.class, 
            "SELECT name, salary, RANK() OVER (ORDER BY salary DESC) AS rank FROM employees");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
    }

    @Test
    void parsesQueryWithNamedWindow() {
        var result = ctx.parse(Query.class, 
            "SELECT name, AVG(salary) OVER w FROM employees WINDOW w AS (PARTITION BY dept)");
        assertTrue(result.ok());
        var query = result.value();
        assertInstanceOf(SelectQuery.class, query);
        SelectQuery select = (SelectQuery) query;
        assertFalse(select.windows().isEmpty());
    }
}
