package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.render.DefaultSqlWriter;
import io.cherlabs.sqm.render.ansi.spi.AnsiDialect;
import io.cherlabs.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpressionColumnRendererTest {

    @Test
    @DisplayName("CASE WHEN C1 IS NULL THEN 1 ELSE 0 END AS Alias")
    void render_expr_with_alias() {
        var column = Column.expr("CASE WHEN C1 IS NULL THEN 1 ELSE 0 END").as("Alias");
        var renderer = new ExpressionColumnRenderer();
        var context = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(context);
        renderer.render(column, context, writer);
        assertEquals("CASE WHEN C1 IS NULL THEN 1 ELSE 0 END AS Alias", writer.toText(List.of()).sql());
    }

    @Test
    @DisplayName("CASE WHEN C1 IS NULL THEN 1 ELSE 0 END AS Order")
    void render_expr_with_aliasAsKeyword() {
        var column = Column.expr("CASE WHEN C1 IS NULL THEN 1 ELSE 0 END").as("Order");
        var renderer = new ExpressionColumnRenderer();
        var context = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(context);
        renderer.render(column, context, writer);
        assertEquals("CASE WHEN C1 IS NULL THEN 1 ELSE 0 END AS \"Order\"", writer.toText(List.of()).sql());
    }
}