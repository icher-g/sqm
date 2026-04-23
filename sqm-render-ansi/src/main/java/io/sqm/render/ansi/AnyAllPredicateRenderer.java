package io.sqm.render.ansi;

import io.sqm.core.AnyAllPredicate;
import io.sqm.core.Quantifier;
import io.sqm.core.QuantifiedSource;
import io.sqm.core.Query;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders quantified comparison predicates with ANY and ALL.
 */
public class AnyAllPredicateRenderer implements Renderer<AnyAllPredicate> {

    private final ComparisonOperatorRenderer operatorRenderer = new ComparisonOperatorRenderer();

    /**
     * Creates an ANY/ALL-predicate renderer.
     */
    public AnyAllPredicateRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(AnyAllPredicate node, RenderContext ctx, SqlWriter w) {
        w.append(node.lhs()).space();
        operatorRenderer.render(node.operator(), ctx, w);
        w.space();
        w.append(node.quantifier() == Quantifier.ALL ? "ALL" : "ANY").space();
        renderSource(node.source(), ctx, w);
    }

    /**
     * Renders the quantified source.
     *
     * @param source the source to render.
     * @param ctx    the render context.
     * @param w      the writer.
     */
    protected void renderSource(QuantifiedSource source, RenderContext ctx, SqlWriter w) {
        if (source instanceof Query query) {
            w.append(query, true, true);
            return;
        }
        throw new UnsupportedOperationException("ANSI renderer supports only query sources for ANY/ALL predicates");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<AnyAllPredicate> targetType() {
        return AnyAllPredicate.class;
    }
}
