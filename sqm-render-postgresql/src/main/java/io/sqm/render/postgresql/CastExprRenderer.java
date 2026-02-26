package io.sqm.render.postgresql;

import io.sqm.core.ArithmeticExpr;
import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.CastExpr;
import io.sqm.core.Expression;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders PostgreSQL {@code ::} cast expressions.
 */
public class CastExprRenderer implements Renderer<CastExpr> {
    /**
     * Creates a PostgreSQL cast-expression renderer.
     */
    public CastExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(CastExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.POSTGRES_TYPECAST)) {
            throw new UnsupportedDialectFeatureException("PostgreSQL :: typecast", ctx.dialect().name());
        }
        w.append(node.expr(), isComposite(node.expr()));
        w.append("::");
        w.append(node.type());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends CastExpr> targetType() {
        return CastExpr.class;
    }

    private boolean isComposite(Expression expr) {
        return expr instanceof ArithmeticExpr || expr instanceof BinaryOperatorExpr;
    }
}
