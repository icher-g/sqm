package io.sqm.render.ansi;

import io.sqm.core.DistinctSpec;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistinctSpecRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());
    private final DistinctSpecRenderer renderer = new DistinctSpecRenderer();

    @Test
    void rendersDistinctKeyword() {
        var w = new DefaultSqlWriter(ctx);
        renderer.render(DistinctSpec.TRUE, ctx, w);
        assertEquals("DISTINCT", w.toText(null).sql());
    }

    @Test
    void distinctOnThrowsForAnsi() {
        var node = DistinctSpec.on(List.of(col("a")));
        var w = new DefaultSqlWriter(ctx);
        var ex = assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(node, ctx, w));
        assertTrue(ex.getMessage().contains("DISTINCT ON"));
    }
}
