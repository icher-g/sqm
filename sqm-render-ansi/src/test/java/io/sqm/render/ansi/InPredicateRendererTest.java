package io.sqm.render.ansi;

import io.sqm.core.InPredicate;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InPredicateRendererTest {

    private final Renderer<InPredicate> renderer = new InPredicateRenderer();

    @Test
    void in_tuples() {
        RenderContext ctx = RenderContext.of(new AnsiDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);
        var p = rows(row(1, "a"), row(2, "b"));
        renderer.render(row(col("c"), col("b")).in(p), ctx, w);
        assertEquals("(c, b) IN ((1, 'a'), (2, 'b'))", w.toText(List.of()).sql());
    }

    @Test
    void notIn_tuples() {
        RenderContext ctx = RenderContext.of(new AnsiDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);
        var p = rows(row("x", 1), row("y", 2));
        renderer.render(row(col("c"), col("b")).notIn(p), ctx, w);

        assertEquals("(c, b) NOT IN (('x', 1), ('y', 2))", w.toText(List.of()).sql());
    }
}