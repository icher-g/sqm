package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Join;
import io.cherlabs.sqlmodel.render.DefaultSqlWriter;
import io.cherlabs.sqlmodel.render.ansi.spi.AnsiRenderContext;
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
        var context = new AnsiRenderContext();
        var writer = new DefaultSqlWriter(context);
        renderer.render(join, context, writer);
        assertEquals("INNER JOIN t2 ON t1.id = t2.id", writer.toText(List.of()).sql());
    }
}