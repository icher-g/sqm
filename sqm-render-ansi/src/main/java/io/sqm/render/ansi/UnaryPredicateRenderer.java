package io.sqm.render.ansi;

import io.sqm.core.UnaryPredicate;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class UnaryPredicateRenderer implements Renderer<UnaryPredicate> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(UnaryPredicate node, RenderContext ctx, SqlWriter w) {
        w.append(node.expr());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends UnaryPredicate> targetType() {
        return UnaryPredicate.class;
    }
}
