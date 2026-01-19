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

        switch (node.mode()) {
            case LIKE -> w.append(node.negated() ? ctx.dialect().operators().notLike() : ctx.dialect().operators().like()).space();
            case ILIKE -> w.append(node.negated() ? ctx.dialect().operators().notIlike() : ctx.dialect().operators().ilike()).space();
            case SIMILAR_TO -> w.append(node.negated() ? ctx.dialect().operators().notSimilarTo() : ctx.dialect().operators().similarTo()).space();
        }
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
