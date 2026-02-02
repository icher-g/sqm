package io.sqm.render.ansi;

import io.sqm.core.AtTimeZoneExpr;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Renderer tests for {@link AtTimeZoneExpr}.
 *
 * <p>AT TIME ZONE is a PostgreSQL-specific extension to ANSI SQL and is NOT
 * supported by the ANSI renderer. These tests verify that the renderer correctly
 * rejects rendering AT TIME ZONE expressions with appropriate error messages.</p>
 *
 * <p>The renderer exists to provide clear feature validation errors, preventing
 * accidental rendering of unsupported SQL constructs.</p>
 */
class AtTimeZoneExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    /**
     * Tests that rendering AT TIME ZONE throws UnsupportedOperationException
     * with a clear error message about the feature not being supported.
     */
    @Test
    void throwsUnsupportedExceptionForAtTimeZone() {
        var expr = AtTimeZoneExpr.of(col("created_at"), lit("UTC"));

        var exception = assertThrows(UnsupportedOperationException.class,
            () -> ctx.render(expr).sql(),
            "Expected UnsupportedOperationException when rendering AT TIME ZONE");

        assertNotNull(exception.getMessage());
        String msg = exception.getMessage().toUpperCase();
        assertTrue(msg.contains("AT TIME ZONE"),
            "Error message should mention AT TIME ZONE");
        assertTrue(msg.contains("DIALECT") || msg.contains("NOT SUPPORTED"),
            "Error message should explain unsupported feature");
    }

    /**
     * Tests that AT TIME ZONE in a WHERE clause throws the expected exception.
     */
    @Test
    void throwsWhenRenderingInWhereClause() {
        var query = select(col("*"))
            .from(tbl("events"))
            .where(col("timestamp").atTimeZone(lit("UTC")).eq(lit("2024-01-01")));

        var exception = assertThrows(UnsupportedOperationException.class,
            () -> ctx.render(query).sql(),
            "Expected error when rendering AT TIME ZONE in WHERE clause");

        assertTrue(exception.getMessage().contains("AT TIME ZONE"));
    }

    /**
     * Tests that AT TIME ZONE in SELECT list throws the expected exception.
     */
    @Test
    void throwsWhenRenderingInSelectList() {
        var query = select(col("created_at").atTimeZone(lit("UTC")))
            .from(tbl("events"));

        var exception = assertThrows(UnsupportedOperationException.class,
            () -> ctx.render(query).sql(),
            "Expected error when rendering AT TIME ZONE in SELECT list");

        assertTrue(exception.getMessage().contains("AT TIME ZONE"));
    }

    /**
     * Tests that nested AT TIME ZONE expressions throw the expected exception.
     */
    @Test
    void throwsWhenRenderingNestedAtTimeZone() {
        var expr = col("timestamp")
            .atTimeZone(col("timestamp").atTimeZone(lit("UTC")));

        var exception = assertThrows(UnsupportedOperationException.class,
            () -> ctx.render(expr).sql(),
            "Expected error when rendering nested AT TIME ZONE");

        assertTrue(exception.getMessage().contains("AT TIME ZONE"));
    }

    /**
     * Tests that AT TIME ZONE with complex timezone expression throws exception.
     */
    @Test
    void throwsWithComplexTimezoneExpression() {
        var expr = col("created_at")
            .atTimeZone(func("get_timezone", arg(col("user_id"))));

        var exception = assertThrows(UnsupportedOperationException.class,
            () -> ctx.render(expr).sql(),
            "Expected error when rendering AT TIME ZONE with function call");

        assertTrue(exception.getMessage().contains("AT TIME ZONE"));
    }
}
