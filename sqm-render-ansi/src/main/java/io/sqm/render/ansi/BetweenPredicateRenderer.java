package io.sqm.render.ansi;

import io.sqm.core.BetweenPredicate;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class BetweenPredicateRenderer implements Renderer<BetweenPredicate> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(BetweenPredicate node, RenderContext ctx, SqlWriter w) {
        var ops = ctx.dialect().operators();
        w.append(node.value()).space();
        if (node.negated()) w.append(ops.not()).space();
        w.append(ops.between()).space();
        w.append(node.lower()).space();
        w.append(ops.and()).space();
        w.append(node.upper());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<BetweenPredicate> targetType() {
        return BetweenPredicate.class;
    }
}
