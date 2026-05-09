package io.sqm.render.ansi;

import io.sqm.core.SequenceValueExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders sequence value expressions, or rejects them when unsupported.
 */
public class SequenceValueExprRenderer implements Renderer<SequenceValueExpr> {
    /**
     * Creates a sequence value expression renderer.
     */
    public SequenceValueExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(SequenceValueExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.SEQUENCE_VALUE_EXPRESSION)) {
            throw new UnsupportedDialectFeatureException(SqlFeature.SEQUENCE_VALUE_EXPRESSION.description(), ctx.dialect().name());
        }
        throw new UnsupportedDialectFeatureException(SqlFeature.SEQUENCE_VALUE_EXPRESSION.description(), ctx.dialect().name());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler
     */
    @Override
    public Class<SequenceValueExpr> targetType() {
        return SequenceValueExpr.class;
    }
}
