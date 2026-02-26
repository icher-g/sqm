package io.sqm.render.ansi;

import io.sqm.core.InPredicate;
import io.sqm.core.QueryExpr;
import io.sqm.core.RowExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders IN and NOT IN predicates.
 */
public class InPredicateRenderer implements Renderer<InPredicate> {
    /**
     * Creates an IN-predicate renderer.
     */
    public InPredicateRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(InPredicate node, RenderContext ctx, SqlWriter w) {
        w.append(node.lhs(), node.lhs() instanceof RowExpr).space();
        w.append(node.negated() ? ctx.dialect().operators().notIn() : ctx.dialect().operators().in()).space();
        w.append(node.rhs(), !(node.rhs() instanceof QueryExpr));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<InPredicate> targetType() {
        return InPredicate.class;
    }
}
