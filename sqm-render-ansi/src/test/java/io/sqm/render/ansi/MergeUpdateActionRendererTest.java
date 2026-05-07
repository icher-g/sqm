package io.sqm.render.ansi;

import io.sqm.core.MergeUpdateAction;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MergeUpdateActionRendererTest {

    @Test
    void rejectsMergeUpdateByDefault() {
        var renderer = new MergeUpdateActionRenderer();
        var ctx = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(
            UnsupportedDialectFeatureException.class,
            () -> renderer.render(MergeUpdateAction.of(java.util.List.of(set("name", col("src", "name")))), ctx, writer)
        );
    }

    @Test
    void rendersSharedMergeUpdateActionThroughSupportedHook() {
        var renderer = new SupportedMergeUpdateActionRenderer();
        var ctx = RenderContext.of(new AnsiDialect());
        var action = MergeUpdateAction.of(java.util.List.of(set("name", col("src", "name"))));

        assertEquals("UPDATE SET name = src.name", render(renderer, action, ctx));
    }

    @Test
    void exposesMergeUpdateActionTargetType() {
        assertEquals(MergeUpdateAction.class, new MergeUpdateActionRenderer().targetType());
    }

    private static String render(SupportedMergeUpdateActionRenderer renderer, MergeUpdateAction action, RenderContext ctx) {
        var writer = new DefaultSqlWriter(ctx);
        renderer.render(action, ctx, writer);
        return writer.toText(java.util.List.of()).sql().replaceAll("\\s+", " ").trim();
    }

    private static final class SupportedMergeUpdateActionRenderer extends MergeUpdateActionRenderer {
        @Override
        public void render(MergeUpdateAction node, RenderContext ctx, SqlWriter w) {
            renderSupportedAction(node, w);
        }
    }
}
