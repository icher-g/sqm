package io.sqm.render.ansi;

import io.sqm.core.VariableTableRef;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Rejects variable-table references for dialects that do not support them.
 */
public class VariableTableRefRenderer implements Renderer<VariableTableRef> {

    /**
     * Creates a variable-table renderer.
     */
    public VariableTableRefRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(VariableTableRef node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("Variable tables", ctx.dialect().name());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<VariableTableRef> targetType() {
        return VariableTableRef.class;
    }
}
