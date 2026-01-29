package io.sqm.render.postgresql;

import io.sqm.core.DistinctSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class DistinctSpecRenderer implements Renderer<DistinctSpec> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(DistinctSpec node, RenderContext ctx, SqlWriter w) {
        w.append("DISTINCT");
        if (!node.items().isEmpty()) {
            w.space().append("ON");
            w.space().append("(");
            w.comma(node.items());
            w.append(")");
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends DistinctSpec> targetType() {
        return DistinctSpec.class;
    }
}
