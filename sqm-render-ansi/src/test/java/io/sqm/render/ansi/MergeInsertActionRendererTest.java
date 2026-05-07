package io.sqm.render.ansi;

import io.sqm.core.MergeInsertAction;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MergeInsertActionRendererTest {

    @Test
    void rejectsMergeInsertByDefault() {
        var renderer = new MergeInsertActionRenderer();
        var ctx = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(
            UnsupportedDialectFeatureException.class,
            () -> renderer.render(MergeInsertAction.of(java.util.List.of(id("id")), row(col("src", "id"))), ctx, writer)
        );
    }

    @Test
    void rendersSharedMergeInsertActionThroughSupportedHook() {
        var renderer = new SupportedMergeInsertActionRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertEquals(
            "INSERT (id, name) VALUES (src.id, src.name)",
            render(renderer, MergeInsertAction.of(java.util.List.of(id("id"), id("name")), row(col("src", "id"), col("src", "name"))), ctx)
        );
        assertEquals(
            "INSERT VALUES (src.id)",
            render(renderer, MergeInsertAction.of(java.util.List.of(), row(col("src", "id"))), ctx)
        );
    }

    @Test
    void exposesMergeInsertActionTargetType() {
        assertEquals(MergeInsertAction.class, new MergeInsertActionRenderer().targetType());
    }

    private static String render(SupportedMergeInsertActionRenderer renderer, MergeInsertAction action, RenderContext ctx) {
        var writer = new DefaultSqlWriter(ctx);
        renderer.render(action, ctx, writer);
        return writer.toText(java.util.List.of()).sql().replaceAll("\\s+", " ").trim();
    }

    private static final class SupportedMergeInsertActionRenderer extends MergeInsertActionRenderer {
        @Override
        public void render(MergeInsertAction node, RenderContext ctx, SqlWriter w) {
            renderSupportedAction(node, ctx, w);
        }
    }
}
