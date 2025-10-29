package io.sqm.render.ansi;

import io.sqm.core.NotPredicate;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class NotPredicateRenderer implements Renderer<NotPredicate> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(NotPredicate node, RenderContext ctx, SqlWriter w) {
        w.append(ctx.dialect().operators().not()).space().append(node.inner(), true);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<NotPredicate> targetType() {
        return NotPredicate.class;
    }
}
