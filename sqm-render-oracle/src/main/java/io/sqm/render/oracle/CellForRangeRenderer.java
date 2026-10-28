package io.sqm.render.oracle;

import io.sqm.core.CellFor;
import io.sqm.core.RangeDirection;
import io.sqm.core.oracle.internal.ModelSyntaxChecker;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code FOR product LIKE 'P%' FROM 1 TO 3 INCREMENT 1} through registered child renderers.
 */
public class CellForRangeRenderer extends io.sqm.render.ansi.CellForRangeRenderer {
    /**
     * Creates an Oracle CellFor.Range renderer.
     */
    public CellForRangeRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellFor.Range node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        ModelSyntaxChecker.range(node);
        w.append("FOR ").append(renderIdentifier(node.dimension(), ctx.dialect().quoter()));
        if (node.likePattern() != null) w.append(" LIKE ").append(node.likePattern());
        w.append(" FROM ").append(node.from()).append(" TO ").append(node.to())
            .append(node.direction() == RangeDirection.INCREMENT ? " INCREMENT " : " DECREMENT ").append(node.step());
    }
}
