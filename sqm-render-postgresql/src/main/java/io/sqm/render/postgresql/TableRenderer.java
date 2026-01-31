package io.sqm.render.postgresql;

import io.sqm.core.Table;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * PostgreSQL renderer for tables supporting ONLY and inheritance star.
 */
public class TableRenderer implements Renderer<Table> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(Table node, RenderContext ctx, SqlWriter w) {
        var quoter = ctx.dialect().quoter();
        var schema = node.schema();
        if (node.inheritance() == Table.Inheritance.ONLY) {
            w.append("ONLY").space();
        }
        if (schema != null && !schema.isBlank()) {
            w.append(quoter.quoteIfNeeded(schema));
            w.append(".");
        }
        w.append(quoter.quoteIfNeeded(node.name()));
        if (node.inheritance() == Table.Inheritance.INCLUDE_DESCENDANTS) {
            w.space().append("*");
        }

        var alias = node.alias();
        if (alias != null && !alias.isBlank()) {
            w.space().append("AS").space().append(quoter.quoteIfNeeded(alias));
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<Table> targetType() {
        return Table.class;
    }
}
