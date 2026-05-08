package io.sqm.render.oracle;

import io.sqm.core.NamedParamExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders Oracle named bind parameters.
 */
public class NamedParamExprRenderer implements Renderer<NamedParamExpr> {

    /**
     * Creates an Oracle named-parameter renderer.
     */
    public NamedParamExprRenderer() {
    }

    @Override
    public void render(NamedParamExpr node, RenderContext ctx, SqlWriter w) {
        w.append(":").append(node.name());
    }

    @Override
    public Class<NamedParamExpr> targetType() {
        return NamedParamExpr.class;
    }
}
