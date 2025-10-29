package io.sqm.render.ansi;

import io.sqm.core.LikePredicate;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class LikePredicateRenderer implements Renderer<LikePredicate> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(LikePredicate node, RenderContext ctx, SqlWriter w) {
        w.append(node.value()).space();
        w.append(node.negated() ? ctx.dialect().operators().notLike() : ctx.dialect().operators().like()).space();
        w.append(node.pattern());

        if (node.escape() != null) {
            w.space().append("ESCAPE").space().append(node.escape());
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<LikePredicate> targetType() {
        return LikePredicate.class;
    }
}
