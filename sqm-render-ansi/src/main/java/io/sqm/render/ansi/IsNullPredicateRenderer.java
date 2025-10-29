package io.sqm.render.ansi;

import io.sqm.core.IsNullPredicate;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class IsNullPredicateRenderer implements Renderer<IsNullPredicate> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(IsNullPredicate node, RenderContext ctx, SqlWriter w) {
        w.append(node.expr()).space();
        w.append(node.negated() ? ctx.dialect().operators().isNotNull() : ctx.dialect().operators().isNull());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<IsNullPredicate> targetType() {
        return IsNullPredicate.class;
    }
}
