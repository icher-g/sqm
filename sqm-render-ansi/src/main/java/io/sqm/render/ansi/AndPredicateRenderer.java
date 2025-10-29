package io.sqm.render.ansi;

import io.sqm.core.AndPredicate;
import io.sqm.core.CompositePredicate;
import io.sqm.core.Predicate;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class AndPredicateRenderer implements Renderer<AndPredicate> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(AndPredicate node, RenderContext ctx, SqlWriter w) {
        w.append(node.lhs(), requireParenthesis(node.lhs()));
        w.space().append(ctx.dialect().operators().and()).space();
        w.append(node.rhs(), requireParenthesis(node.rhs()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<AndPredicate> targetType() {
        return AndPredicate.class;
    }

    private boolean requireParenthesis(Predicate predicate) {
        return !(predicate instanceof AndPredicate) && predicate instanceof CompositePredicate;
    }
}
