package io.sqm.render.sqlserver;

import io.sqm.core.VariableTableRef;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders SQL Server table-variable references such as {@code @audit}.
 */
public class VariableTableRefRenderer implements Renderer<VariableTableRef> {

    /**
     * Creates a SQL Server table-variable renderer.
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
        w.append("@").append(node.name().value());
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
