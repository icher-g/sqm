package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Column;
import io.cherlabs.sqlmodel.core.Filter;
import io.cherlabs.sqlmodel.render.DefaultSqlWriter;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.ansi.spi.AnsiRenderContext;
import io.cherlabs.sqlmodel.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TupleFilterRendererTest {

    private final TupleFilterRenderer renderer = new TupleFilterRenderer();

    @Test
    void in_tuples() {
        RenderContext ctx = new AnsiRenderContext();
        List<List<Object>> rows = List.of(List.of(1, "a"), List.of(2, "b"));

        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(Filter.tuple(List.of(Column.of("c"), Column.of("b"))).in(rows), ctx, w);

        assertEquals("(c, b) IN ((1, 'a'), (2, 'b'))", w.toText(List.of()).sql());
    }

    @Test
    void notIn_tuples() {
        RenderContext ctx = new AnsiRenderContext();
        List<List<Object>> rows = List.of(List.of("x", 1), List.of("y", 2));

        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(Filter.tuple(List.of(Column.of("c"), Column.of("b"))).notIn(rows), ctx, w);

        assertEquals("(c, b) NOT IN (('x', 1), ('y', 2))", w.toText(List.of()).sql());
    }
}