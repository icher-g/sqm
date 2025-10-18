package io.sqm.render.ansi;

import io.sqm.core.Join;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.ansi.join.ExpressionJoinRenderer;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpressionJoinRendererTest {

    @Test
    @DisplayName("INNER JOIN -> INNER JOIN <table> ON <predicate>")
    void render_expr() {
        var join = Join.expr("INNER JOIN t2 ON t1.id = t2.id");
        var renderer = new ExpressionJoinRenderer();
        var context = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(context);
        renderer.render(join, context, writer);
        assertEquals("INNER JOIN t2 ON t1.id = t2.id", writer.toText(List.of()).sql());
    }
}