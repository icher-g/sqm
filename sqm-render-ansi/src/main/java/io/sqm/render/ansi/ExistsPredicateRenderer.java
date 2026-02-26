package io.sqm.render.ansi;

import io.sqm.core.ExistsPredicate;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders EXISTS predicates.
 */
public class ExistsPredicateRenderer implements Renderer<ExistsPredicate> {
    /**
     * Creates an EXISTS-predicate renderer.
     */
    public ExistsPredicateRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ExistsPredicate node, RenderContext ctx, SqlWriter w) {
        if (node.negated()) {
            w.append("NOT").space();
        }
        w.append("EXISTS").space();
        w.append(node.subquery(), true, true);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ExistsPredicate> targetType() {
        return ExistsPredicate.class;
    }
}
