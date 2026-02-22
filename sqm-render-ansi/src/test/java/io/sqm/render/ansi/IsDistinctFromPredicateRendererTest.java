package io.sqm.render.ansi;

import io.sqm.core.IsDistinctFromPredicate;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IsDistinctFromPredicateRenderer}.
 */
class IsDistinctFromPredicateRendererTest {

    private RenderContext ctx;

    @BeforeEach
    void setUp() {
        ctx = RenderContext.of(new AnsiDialect());
    }

    @Test
    void testRenderIsDistinctFrom() {
        var predicate = IsDistinctFromPredicate.of(col("a"), col("b"), false);
        var sql = ctx.render(predicate).sql();

        assertEquals("a IS DISTINCT FROM b", sql);
    }

    @Test
    void testRenderIsNotDistinctFrom() {
        var predicate = IsDistinctFromPredicate.of(col("x"), col("y"), true);
        var sql = ctx.render(predicate).sql();

        assertEquals("x IS NOT DISTINCT FROM y", sql);
    }

    @Test
    void testRenderWithLiteral() {
        var predicate = IsDistinctFromPredicate.of(col("status"), lit("active"), false);
        var sql = ctx.render(predicate).sql();

        assertEquals("status IS DISTINCT FROM 'active'", sql);
    }

    @Test
    void testRenderWithNull() {
        var predicate = IsDistinctFromPredicate.of(col("value"), lit(null), false);
        var sql = ctx.render(predicate).sql();

        assertEquals("value IS DISTINCT FROM NULL", sql);
    }

    @Test
    void testRenderWithNumericLiteral() {
        var predicate = IsDistinctFromPredicate.of(col("count"), lit(42), true);
        var sql = ctx.render(predicate).sql();

        assertEquals("count IS NOT DISTINCT FROM 42", sql);
    }

    @Test
    void testRenderWithQualifiedColumns() {
        var predicate = IsDistinctFromPredicate.of(
            col("t1", "id"),
            col("t2", "id"),
            false
        );
        var sql = ctx.render(predicate).sql();

        assertEquals("t1.id IS DISTINCT FROM t2.id", sql);
    }

    @Test
    void testRenderWithArithmeticExpressions() {
        var lhs = col("a").add(lit(1));
        var rhs = col("b").mul(lit(2));
        var predicate = IsDistinctFromPredicate.of(lhs, rhs, false);
        var sql = ctx.render(predicate).sql();

        assertEquals("a + 1 IS DISTINCT FROM b * 2", sql);
    }

    @Test
    void testRenderWithFunctionCall() {
        var lhs = func("UPPER", arg(col("name")));
        var predicate = IsDistinctFromPredicate.of(lhs, lit("JOHN"), true);
        var sql = ctx.render(predicate).sql();

        assertEquals("UPPER(name) IS NOT DISTINCT FROM 'JOHN'", sql);
    }

    @Test
    void testRenderInAndPredicate() {
        var pred1 = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        var pred2 = col("b").eq(lit(2));
        var andPred = pred1.and(pred2);
        
        var sql = ctx.render(andPred).sql();

        assertEquals("a IS DISTINCT FROM 1 AND b = 2", sql);
    }

    @Test
    void testRenderInOrPredicate() {
        var pred1 = IsDistinctFromPredicate.of(col("x"), lit(null), true);
        var pred2 = col("y").isNull();
        var orPred = pred1.or(pred2);
        
        var sql = ctx.render(orPred).sql();

        assertEquals("x IS NOT DISTINCT FROM NULL OR y IS NULL", sql);
    }

    @Test
    void testRenderInNotPredicate() {
        var pred = IsDistinctFromPredicate.of(col("a"), col("b"), false);
        var notPred = pred.not();
        
        var sql = ctx.render(notPred).sql();

        assertEquals("NOT (a IS DISTINCT FROM b)", sql);
    }

    @Test
    void testRenderInSelectQuery() {
        var query = select(col("id"), col("name"))
            .from(tbl("users"))
            .where(IsDistinctFromPredicate.of(col("status"), lit("active"), false))
            .build();
        
        var sql = ctx.render(query).sql();

        assertTrue(sql.contains("WHERE status IS DISTINCT FROM 'active'"));
    }

    @Test
    void testRenderInJoinCondition() {
        var join = inner(tbl("orders").as("o"))
            .on(IsDistinctFromPredicate.of(col("u", "id"), col("o", "user_id"), true));
        
        var query = select(col("u", "name"))
            .from(tbl("users").as("u"))
            .join(join)
            .build();
        
        var sql = ctx.render(query).sql();

        assertTrue(sql.contains("ON u.id IS NOT DISTINCT FROM o.user_id"));
    }

    @Test
    void testRenderInHavingClause() {
        var query = select(col("dept"), func("COUNT", starArg()).as("cnt"))
            .from(tbl("employees"))
            .groupBy(group("dept"))
            .having(IsDistinctFromPredicate.of(
                func("COUNT", starArg()),
                lit(10),
                false
            ))
            .build();
        
        var sql = ctx.render(query).sql();

        assertTrue(sql.contains("HAVING COUNT(*) IS DISTINCT FROM 10"));
    }

    @Test
    void testRendererTargetType() {
        var renderer = new IsDistinctFromPredicateRenderer();
        assertEquals(IsDistinctFromPredicate.class, renderer.targetType());
    }

    @Test
    void testRenderWithCastExpression() {
        var lhs = col("created_at");
        var rhs = func("CAST", arg(lit("2024-01-01")));
        var predicate = IsDistinctFromPredicate.of(lhs, rhs, true);
        
        var sql = ctx.render(predicate).sql();

        assertTrue(sql.contains("IS NOT DISTINCT FROM"));
    }

    @Test
    void testRenderWithSubquery() {
        var subquery = select(col("id")).from(tbl("temp")).build();
        var predicate = IsDistinctFromPredicate.of(
            col("main_id"),
            expr(subquery),
            false
        );
        
        var sql = ctx.render(predicate).sql();

        assertTrue(sql.contains("IS DISTINCT FROM"));
        assertTrue(sql.contains("SELECT id"));
        assertTrue(sql.contains("FROM temp"));
    }

    @Test
    void testRenderComplexExpression() {
        var lhs = col("price").mul(col("quantity"));
        var rhs = col("total");
        var predicate = IsDistinctFromPredicate.of(lhs, rhs, false);
        
        var sql = ctx.render(predicate).sql();

        assertEquals("price * quantity IS DISTINCT FROM total", sql);
    }

    @Test
    void testRenderWithParentheses() {
        var innerPred = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        var outerPred = innerPred.and(col("b").eq(lit(2)));
        
        var sql = ctx.render(outerPred).sql();

        assertEquals("a IS DISTINCT FROM 1 AND b = 2", sql);
    }
}
