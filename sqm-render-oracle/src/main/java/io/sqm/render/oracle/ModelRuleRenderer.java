package io.sqm.render.oracle;

import io.sqm.core.ModelRule;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders basic Oracle MODEL ModelRule syntax through registered child renderers.
 */
public class ModelRuleRenderer extends io.sqm.render.ansi.ModelRuleRenderer {
    /**
     * Creates an Oracle ModelRule renderer.
     */
    public ModelRuleRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ModelRule node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        if (node.orderBy() != null) throw ModelRenderSupport.unsupported("ordered rules", ctx);
        if (node.mode() != null) w.append(ModelRenderSupport.mode(node.mode())).space();
        w.append(node.target()).append(" = ").append(node.value());
    }
}
