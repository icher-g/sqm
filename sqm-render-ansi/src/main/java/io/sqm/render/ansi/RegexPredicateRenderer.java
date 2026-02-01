package io.sqm.render.ansi;

import io.sqm.core.RegexPredicate;
import io.sqm.core.dialect.SqlFeature;
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
        if (!ctx.dialect().capabilities().supports(SqlFeature.REGEX_PREDICATE)) {
            throw new UnsupportedDialectFeatureException("Regex predicate", ctx.dialect().name());
        }
        w.append(node.value()).space();
        w.append(node.negated() ? "!" : "");

        switch (node.mode()) {
            case MATCH -> w.append("~");
            case MATCH_INSENSITIVE -> w.append("~*");
        }

        w.space().append(node.pattern());
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
