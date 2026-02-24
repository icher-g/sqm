package io.sqm.render.ansi;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.OperatorName;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class BinaryOperatorExprRenderer implements Renderer<BinaryOperatorExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(BinaryOperatorExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.CUSTOM_OPERATOR)) {
            throw new UnsupportedDialectFeatureException("Binary operator " + node.operator().text(), ctx.dialect().name());
        }
        w.append(node.left()).space();
        renderOperator(node.operator(), ctx, w);
        w.space().append(node.right());
    }

    private void renderOperator(OperatorName operatorName, RenderContext ctx, SqlWriter w) {
        if (!operatorName.operatorKeywordSyntax()) {
            w.append(operatorName.symbol());
            return;
        }

        w.append("OPERATOR(");
        if (operatorName.schemaName() != null) {
            var quoter = ctx.dialect().quoter();
            var parts = operatorName.schemaName().parts();
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) {
                    w.append(".");
                }
                w.append(renderIdentifier(parts.get(i), quoter));
            }
            w.append(".");
        }
        w.append(operatorName.symbol()).append(")");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends BinaryOperatorExpr> targetType() {
        return BinaryOperatorExpr.class;
    }
}
