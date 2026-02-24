package io.sqm.render.ansi;

import io.sqm.core.CollateExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders expression-level {@code COLLATE} constructs.
 */
public class CollateExprRenderer implements Renderer<CollateExpr> {

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(CollateExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.EXPR_COLLATE)) {
            throw new UnsupportedDialectFeatureException("COLLATE", ctx.dialect().name());
        }
        var quoter = ctx.dialect().quoter();
        w.append(node.expr())
            .space().append("COLLATE").space()
            .append(renderQualifiedName(node.collation(), quoter));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends CollateExpr> targetType() {
        return CollateExpr.class;
    }

    private String renderQualifiedName(io.sqm.core.QualifiedName name, io.sqm.render.spi.IdentifierQuoter quoter) {
        return name.parts().stream()
            .map(part -> renderIdentifier(part, quoter))
            .collect(java.util.stream.Collectors.joining("."));
    }
}
