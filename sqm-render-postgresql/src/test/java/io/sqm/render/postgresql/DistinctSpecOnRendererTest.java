package io.sqm.render.postgresql;

import io.sqm.core.DistinctSpec;
import io.sqm.render.ansi.DistinctSpecRenderer;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DistinctSpecOnRendererTest {

    private final RenderContext ctx = RenderContext.of(new PostgresDialect());
    private final DistinctSpecRenderer renderer = new DistinctSpecRenderer();

    @Test
    void rendersDistinctOnItems() {
        var spec = DistinctSpec.on(List.of(col("a"), col("b")));
        var w = new DefaultSqlWriter(ctx);
        renderer.render(spec, ctx, w);
        assertEquals("DISTINCT ON (a, b)", w.toText(null).sql());
    }
}
