package io.sqm.render.postgresql;

import io.sqm.core.Expression;
import io.sqm.core.QuantifiedSource;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * PostgreSQL renderer for quantified ANY/ALL predicates.
 */
public class AnyAllPredicateRenderer extends io.sqm.render.ansi.AnyAllPredicateRenderer {

    /**
     * Creates a PostgreSQL ANY/ALL predicate renderer.
     */
    public AnyAllPredicateRenderer() {
    }

    /**
     * Renders PostgreSQL array-expression sources in addition to standard query sources.
     *
     * @param source the source to render.
     * @param ctx    the render context.
     * @param w      the writer.
     */
    @Override
    protected void renderSource(QuantifiedSource source, RenderContext ctx, SqlWriter w) {
        if (source instanceof Expression expression) {
            w.append(expression, true);
            return;
        }
        super.renderSource(source, ctx, w);
    }
}
