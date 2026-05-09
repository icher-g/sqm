package io.sqm.render.oracle;

import io.sqm.core.SequenceValueExpr;
import io.sqm.core.SequenceValueKind;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle sequence value expressions.
 */
public class SequenceValueExprRenderer extends io.sqm.render.ansi.SequenceValueExprRenderer {
    /**
     * Creates an Oracle sequence value expression renderer.
     */
    public SequenceValueExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render
     * @param ctx  render context
     * @param w    SQL writer
     */
    @Override
    public void render(SequenceValueExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.SEQUENCE_VALUE_EXPRESSION)) {
            throw new UnsupportedDialectFeatureException(SqlFeature.SEQUENCE_VALUE_EXPRESSION.description(), ctx.dialect().name());
        }
        w.append(renderQualifiedName(node.sequence(), ctx.dialect().quoter()))
            .append(node.kind() == SequenceValueKind.NEXT_VALUE ? ".NEXTVAL" : ".CURRVAL");
    }
}
