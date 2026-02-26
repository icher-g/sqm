package io.sqm.render.ansi;

import io.sqm.core.ComparisonPredicate;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders comparison predicates.
 */
public class ComparisonPredicateRenderer implements Renderer<ComparisonPredicate> {

    private final ComparisonOperatorRenderer operatorRenderer = new ComparisonOperatorRenderer();

    /**
     * Creates a comparison-predicate renderer.
     */
    public ComparisonPredicateRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ComparisonPredicate node, RenderContext ctx, SqlWriter w) {
        w.append(node.lhs()).space();
        operatorRenderer.render(node.operator(), ctx, w);
        w.space().append(node.rhs());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ComparisonPredicate> targetType() {
        return ComparisonPredicate.class;
    }
}
