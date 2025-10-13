package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.render.DefaultSqlWriter;
import io.cherlabs.sqm.render.ansi.spi.AnsiDialect;
import io.cherlabs.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpressionFilterRendererTest {

    @Test
    @DisplayName("CASE WHEN C1 IS NULL THEN 1 ELSE 0 END == 1")
    void render_expr() {
        var expression = Filter.expr("CASE WHEN C1 IS NULL THEN 1 ELSE 0 END == 1");
        var renderer = new ExpressionFilterRenderer();
        var context = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(context);
        renderer.render(expression, context, writer);
        assertEquals("CASE WHEN C1 IS NULL THEN 1 ELSE 0 END == 1", writer.toText(List.of()).sql());
    }
}