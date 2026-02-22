package io.sqm.render.ansi;

import io.sqm.core.AtTimeZoneExpr;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Renderer tests for {@link AtTimeZoneExpr}.
 *
 * <p>Tests both feature rejection (ANSI) and actual rendering logic (TestDialect with features enabled).</p>
 *
 * <p>AT TIME ZONE is a PostgreSQL-specific extension to ANSI SQL and is NOT
 * supported by the ANSI renderer. These tests verify that the renderer correctly
 * rejects rendering AT TIME ZONE expressions with appropriate error messages when
 * using ANSI dialect, but also verify happy path rendering when the feature is enabled.</p>
 */
@DisplayName("AtTimeZoneExprRenderer Tests")
class AtTimeZoneExprRendererTest {

    private RenderContext ansiCtx;
    private RenderContext testDialectCtx;

    @BeforeEach
    void setUp() {
        ansiCtx = RenderContext.of(new AnsiDialect());
        testDialectCtx = RenderContext.of(new TestDialectWithAllFeatures());
    }

    /**
     * Tests that rendering AT TIME ZONE throws UnsupportedOperationException
     * with a clear error message about the feature not being supported.
     */
    @Test
    void throwsUnsupportedExceptionForAtTimeZone() {
        var expr = AtTimeZoneExpr.of(col("created_at"), lit("UTC"));

        var exception = assertThrows(UnsupportedOperationException.class,
            () -> ansiCtx.render(expr).sql(),
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
            .where(col("timestamp").atTimeZone(lit("UTC")).eq(lit("2024-01-01")))
            .build();

        var exception = assertThrows(UnsupportedOperationException.class,
            () -> ansiCtx.render(query).sql(),
            "Expected error when rendering AT TIME ZONE in WHERE clause");

        assertTrue(exception.getMessage().contains("AT TIME ZONE"));
    }

    /**
     * Tests that AT TIME ZONE in SELECT list throws the expected exception.
     */
    @Test
    void throwsWhenRenderingInSelectList() {
        var query = select(col("created_at").atTimeZone(lit("UTC")))
            .from(tbl("events"))
            .build();

        var exception = assertThrows(UnsupportedOperationException.class,
            () -> ansiCtx.render(query).sql(),
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
            () -> ansiCtx.render(expr).sql(),
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
            () -> ansiCtx.render(expr).sql(),
            "Expected error when rendering AT TIME ZONE with function call");

        assertTrue(exception.getMessage().contains("AT TIME ZONE"));
    }

    /* ==================== HAPPY PATH TESTS (WITH FEATURES ENABLED) ==================== */

    /**
     * Tests AT TIME ZONE rendering when feature is enabled.
     */
    @Test
    @DisplayName("Render AT TIME ZONE with literal timezone when supported")
    void renderAtTimeZoneWithLiteralTimezone() {
        var expr = AtTimeZoneExpr.of(col("created_at"), lit("UTC"));

        String sql = testDialectCtx.render(expr).sql();

        assertNotNull(sql);
        assertTrue(sql.toLowerCase().contains("at time zone") ||
            sql.toLowerCase().contains("attimezone") ||
            sql.toLowerCase().contains("at"), "Should contain AT TIME ZONE reference");
    }

    /**
     * Tests AT TIME ZONE rendering with column reference for timezone.
     */
    @Test
    @DisplayName("Render AT TIME ZONE with column timezone when supported")
    void renderAtTimeZoneWithColumnTimezone() {
        var expr = col("ts_col").atTimeZone(col("tz_col"));

        String sql = testDialectCtx.render(expr).sql();

        assertNotNull(sql);
        assertTrue(sql.contains("ts_col"), "Should contain timestamp column");
        assertTrue(sql.contains("tz_col"), "Should contain timezone column");
    }

    /**
     * Tests AT TIME ZONE in SELECT context when feature is enabled.
     */
    @Test
    @DisplayName("Render AT TIME ZONE in SELECT when supported")
    void renderAtTimeZoneInSelectContext() {
        var query = select(col("created_at").atTimeZone(lit("UTC")))
            .from(tbl("events"))
            .build();

        String sql = testDialectCtx.render(query).sql();

        assertNotNull(sql);
        assertTrue(sql.contains("created_at"), "Should contain column name");
        assertTrue(sql.contains("UTC"), "Should contain timezone literal");
    }

    /**
     * Tests AT TIME ZONE in WHERE clause when feature is enabled.
     */
    @Test
    @DisplayName("Render AT TIME ZONE in WHERE when supported")
    void renderAtTimeZoneInWhereClause() {
        var query = select(col("*"))
            .from(tbl("events"))
            .where(col("timestamp").atTimeZone(lit("UTC")).eq(lit("2024-01-01")))
            .build();

        String sql = testDialectCtx.render(query).sql();

        assertNotNull(sql);
        assertTrue(sql.contains("timestamp"), "Should contain column name");
        assertTrue(sql.contains("UTC"), "Should contain timezone");
    }

    /**
     * Tests nested AT TIME ZONE when feature is enabled.
     */
    @Test
    @DisplayName("Render nested AT TIME ZONE when supported")
    void renderNestedAtTimeZone() {
        var expr = col("created_at")
            .atTimeZone(col("created_at").atTimeZone(lit("UTC")));

        String sql = testDialectCtx.render(expr).sql();

        assertNotNull(sql);
        assertTrue(sql.contains("created_at"), "Should contain column name");
    }

    /**
     * Test dialect that supports all features including AT TIME ZONE.
     */
    private static class TestDialectWithAllFeatures extends AnsiDialect {
        private DialectCapabilities capabilities;

        @Override
        public DialectCapabilities capabilities() {
            if (capabilities == null) {
                var builder = VersionedDialectCapabilities.builder(SqlDialectVersion.of(2016));
                // Enable all features for testing
                for (SqlFeature feature : SqlFeature.values()) {
                    builder.supports(feature);
                }
                capabilities = builder.build();
            }
            return capabilities;
        }
    }
}
