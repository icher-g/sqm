package io.sqm.render.oracle;

import io.sqm.core.IsAnyPredicate;
import io.sqm.core.oracle.internal.ModelSyntaxChecker;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code product IS ANY} through registered child renderers.
 */
public class IsAnyPredicateRenderer extends io.sqm.render.ansi.IsAnyPredicateRenderer {
    /**
     * Creates an Oracle IsAnyPredicate renderer.
     */
    public IsAnyPredicateRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(IsAnyPredicate node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        ModelSyntaxChecker.selector(node);
        if (node.dimension() != null) w.append(node.dimension()).append(" IS ");
        w.append("ANY");
    }
}
