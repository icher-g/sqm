package io.sqm.render.ansi;

import io.sqm.core.ClassifierExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders a row-pattern classifier expression.
 */
public class ClassifierExprRenderer implements Renderer<ClassifierExpr> {
    /**
     * Creates a classifier-expression renderer.
     */
    public ClassifierExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ClassifierExpr node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append("CLASSIFIER(");
        if (node.variable() != null) w.append(renderIdentifier(node.variable(), ctx.dialect().quoter()));
        w.append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ClassifierExpr> targetType() {
        return ClassifierExpr.class;
    }
}
