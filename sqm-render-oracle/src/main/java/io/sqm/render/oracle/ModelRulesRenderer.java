package io.sqm.render.oracle;

import io.sqm.core.ModelRules;
import io.sqm.core.RuleMode;
import io.sqm.core.RuleOrder;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders basic Oracle MODEL ModelRules syntax through registered child renderers.
 */
public class ModelRulesRenderer extends io.sqm.render.ansi.ModelRulesRenderer {
    /**
     * Creates an Oracle ModelRules renderer.
     */
    public ModelRulesRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ModelRules node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        if (node.iteration() != null) w.append(node.iteration());
        w.append("RULES");
        if (node.defaultMode() != RuleMode.UPSERT) w.space().append(ModelRenderSupport.mode(node.defaultMode()));
        if (node.order() == RuleOrder.AUTOMATIC) w.append(" AUTOMATIC ORDER");
        var multiline = node.rules().size() > 1;
        w.append(" (");
        if (multiline) w.newline().indent();
        w.comma(node.rules(), false, multiline);
        if (multiline) w.outdent().newline();
        w.append(")");
    }
}
