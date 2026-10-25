package io.sqm.render.ansi;

import io.sqm.core.IsAnyPredicate;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for IsAnyPredicate. Oracle support follows in #506.
 */
public class IsAnyPredicateRenderer implements Renderer<IsAnyPredicate> {
    /**
     * Creates the IsAnyPredicate render.
     */
    public IsAnyPredicateRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(IsAnyPredicate node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException(ctx.dialect().capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL IsAnyPredicate is not supported yet (#506)" : "MODEL IsAnyPredicate is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<IsAnyPredicate> targetType() {
        return IsAnyPredicate.class;
    }
}
