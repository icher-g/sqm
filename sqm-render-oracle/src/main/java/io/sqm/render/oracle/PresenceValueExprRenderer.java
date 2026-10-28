package io.sqm.render.oracle;

import io.sqm.core.PresenceMode;
import io.sqm.core.PresenceValueExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code PRESENTV(sales[2025], 100, 0)} through registered child renderers.
 */
public class PresenceValueExprRenderer extends io.sqm.render.ansi.PresenceValueExprRenderer {
    /**
     * Creates an Oracle PresenceValueExpr renderer.
     */
    public PresenceValueExprRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(PresenceValueExpr node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        w.append(node.mode() == PresenceMode.CELL ? "PRESENTV(" : "PRESENTNNV(")
            .append(node.cell()).append(", ").append(node.whenPresent()).append(", ").append(node.whenAbsent()).append(")");
    }
}
