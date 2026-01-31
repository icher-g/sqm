package io.sqm.render.postgresql;

import io.sqm.core.DollarStringLiteralExpr;
import io.sqm.core.EscapeStringLiteralExpr;
import io.sqm.core.IntervalLiteralExpr;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExtendedLiteralExprRendererTest {

    @Test
    void renders_escape_string_literal() {
        var ctx = RenderContext.of(new io.sqm.render.postgresql.spi.PostgresDialect());
        var out = ctx.render(EscapeStringLiteralExpr.of("it\\'s"));
        assertEquals("E'it\\'s'", out.sql().trim());
    }

    @Test
    void renders_dollar_quoted_literal() {
        var ctx = RenderContext.of(new io.sqm.render.postgresql.spi.PostgresDialect());
        var out = ctx.render(DollarStringLiteralExpr.of("tag", "value"));
        assertEquals("$tag$value$tag$", out.sql().trim());
    }

    @Test
    void renders_interval_literal_with_qualifier() {
        var ctx = RenderContext.of(new io.sqm.render.postgresql.spi.PostgresDialect());
        var out = ctx.render(IntervalLiteralExpr.of("1", "DAY"));
        assertEquals("INTERVAL '1' DAY", out.sql().trim());
    }
}
