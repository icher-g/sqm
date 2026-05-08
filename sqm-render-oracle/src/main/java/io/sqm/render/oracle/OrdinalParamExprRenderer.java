package io.sqm.render.oracle;

import io.sqm.core.OrdinalParamExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders Oracle ordinal bind parameters.
 */
public class OrdinalParamExprRenderer implements Renderer<OrdinalParamExpr> {

    /**
     * Creates an Oracle ordinal-parameter renderer.
     */
    public OrdinalParamExprRenderer() {
    }

    @Override
    public void render(OrdinalParamExpr node, RenderContext ctx, SqlWriter w) {
        w.append(":").append(Integer.toString(node.index()));
    }

    @Override
    public Class<OrdinalParamExpr> targetType() {
        return OrdinalParamExpr.class;
    }
}
