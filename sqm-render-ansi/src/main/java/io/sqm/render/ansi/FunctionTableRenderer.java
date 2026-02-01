package io.sqm.render.ansi;

import io.sqm.core.FunctionTable;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class FunctionTableRenderer implements Renderer<FunctionTable> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(FunctionTable node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.FUNCTION_TABLE)) {
            throw new UnsupportedDialectFeatureException("Function table", ctx.dialect().name());
        }
        w.append(node.function());
        if (node.ordinality()) {
            if (!ctx.dialect().capabilities().supports(SqlFeature.FUNCTION_TABLE_ORDINALITY)) {
                throw new UnsupportedDialectFeatureException("WITH ORDINALITY", ctx.dialect().name());
            }
            w.space().append("WITH ORDINALITY");
        }
        renderAliased(node, ctx, w);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends FunctionTable> targetType() {
        return FunctionTable.class;
    }
}
