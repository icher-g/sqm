package io.sqm.render.ansi;

import io.sqm.core.CompositePredicate;
import io.sqm.core.OrPredicate;
import io.sqm.core.Predicate;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders OR predicates.
 */
public class OrPredicateRenderer implements Renderer<OrPredicate> {
    /**
     * Creates an OR-predicate renderer.
     */
    public OrPredicateRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OrPredicate node, RenderContext ctx, SqlWriter w) {
        w.append(node.lhs(), requireParenthesis(node.lhs()));
        w.space().append(ctx.dialect().operators().or()).space();
        w.append(node.rhs(), requireParenthesis(node.rhs()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OrPredicate> targetType() {
        return OrPredicate.class;
    }

    private boolean requireParenthesis(Predicate predicate) {
        return !(predicate instanceof OrPredicate) && predicate instanceof CompositePredicate;
    }
}
