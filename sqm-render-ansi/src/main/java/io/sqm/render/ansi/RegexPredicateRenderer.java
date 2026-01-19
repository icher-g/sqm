package io.sqm.render.ansi;

import io.sqm.core.RegexPredicate;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class RegexPredicateRenderer implements Renderer<RegexPredicate> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(RegexPredicate node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("regex", "ANSI");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends RegexPredicate> targetType() {
        return RegexPredicate.class;
    }
}
