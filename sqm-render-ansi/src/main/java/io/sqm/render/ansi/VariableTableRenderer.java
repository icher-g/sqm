package io.sqm.render.ansi;

import io.sqm.core.VariableTable;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Rejects variable-table references for dialects that do not support them.
 */
public class VariableTableRenderer implements Renderer<VariableTable> {

    /**
     * Creates a variable-table renderer.
     */
    public VariableTableRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(VariableTable node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("Variable tables", ctx.dialect().name());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<VariableTable> targetType() {
        return VariableTable.class;
    }
}
