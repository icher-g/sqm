package io.sqm.render.sqlserver;

import io.sqm.core.VariableTable;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders SQL Server table-variable references such as {@code @audit}.
 */
public class VariableTableRenderer implements Renderer<VariableTable> {

    /**
     * Creates a SQL Server table-variable renderer.
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
        w.append("@").append(node.name().value());
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
