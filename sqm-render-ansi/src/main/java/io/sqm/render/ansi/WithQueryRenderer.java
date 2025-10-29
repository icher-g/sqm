package io.sqm.render.ansi;

import io.sqm.core.WithQuery;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class WithQueryRenderer implements Renderer<WithQuery> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(WithQuery node, RenderContext ctx, SqlWriter w) {
        var ctes = node.ctes();
        if (ctes == null || ctes.isEmpty()) {
            throw new IllegalArgumentException("WITH requires at least one query (CTE + outer query).");
        }

        w.append("WITH");
        if (node.recursive()) w.space().append("RECURSIVE");
        w.newline();
        w.indent();

        for (int i = 0; i < ctes.size(); i++) {
            if (i > 0) w.append(",").newline();
            w.append(ctes.get(i));
        }
        w.newline();
        w.outdent();
        w.append(node.body());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<WithQuery> targetType() {
        return WithQuery.class;
    }
}
