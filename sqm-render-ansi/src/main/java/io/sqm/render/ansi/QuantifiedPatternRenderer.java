package io.sqm.render.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders a greedy or reluctant row-pattern quantifier.
 */
public class QuantifiedPatternRenderer implements Renderer<MatchPattern.Quantified> {
    /**
     * Creates a quantified-pattern renderer.
     */
    public QuantifiedPatternRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(MatchPattern.Quantified node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        PatternRenderSupport.renderChild(node.pattern(), 3, ctx, w);
        if (node.minimum() == 0 && node.maximum() == null) w.append("*");
        else {
            if (node.minimum() == 1 && node.maximum() == null) w.append("+");
            else {
                if (node.minimum() == 0 && Integer.valueOf(1).equals(node.maximum())) w.append("?");
                else {
                    w.append("{");
                    if (node.minimum().equals(node.maximum())) {
                        w.append(node.minimum().toString());
                    }
                    else {
                        if (node.minimum() != 0) w.append(node.minimum().toString());
                        w.append(",");
                        if (node.maximum() != null) w.append(node.maximum().toString());
                    }
                    w.append("}");
                }
            }
        }
        if (node.reluctant()) w.append("?");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Quantified> targetType() {
        return MatchPattern.Quantified.class;
    }
}
