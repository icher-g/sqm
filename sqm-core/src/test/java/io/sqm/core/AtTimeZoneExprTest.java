package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AtTimeZoneExpr}.
 */
class AtTimeZoneExprTest {

    @Test
    void shouldCreateUsingFactory() {
        var expr = AtTimeZoneExpr.of(col("ts"), lit("UTC"));
        assertNotNull(expr);
        assertInstanceOf(ColumnExpr.class, expr.timestamp());
        assertInstanceOf(LiteralExpr.class, expr.timezone());
    }

    @Test
    void shouldCreateUsingDslHelper() {
        var expr = col("ts").atTimeZone(lit("UTC"));
        assertNotNull(expr);
        assertInstanceOf(AtTimeZoneExpr.class, expr);
    }

    @Test
    void shouldPreserveTimestamp() {
        var timestamp = col("created_at");
        var expr = timestamp.atTimeZone(lit("America/New_York"));
        assertEquals(timestamp, expr.timestamp());
    }

    @Test
    void shouldPreserveTimezone() {
        var timezone = lit("UTC");
        var expr = col("ts").atTimeZone(timezone);
        assertEquals(timezone, expr.timezone());
    }

    @Test
    void shouldSupportColumnTimezone() {
        var expr = col("ts").atTimeZone(col("tz_column"));
        assertInstanceOf(ColumnExpr.class, expr.timezone());
    }

    @Test
    void shouldSupportFunctionTimezone() {
        var expr = col("ts").atTimeZone(func("get_timezone"));
        assertInstanceOf(FunctionExpr.class, expr.timezone());
    }

    @Test
    void shouldSupportNestedExpression() {
        var expr = col("ts").atTimeZone(col("tz"));
        assertNotNull(expr);
    }

    @Test
    void shouldAcceptVisitor() {
        var expr = col("ts").atTimeZone(lit("UTC"));
        var visitor = new TestVisitor();
        var result = expr.accept(visitor);
        assertTrue(result);
    }

    private static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return true;
        }

        @Override
        public Boolean visitAtTimeZoneExpr(AtTimeZoneExpr expr) {
            return true;
        }
    }
}
