package io.sqm.render.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Dispatches the sealed row-pattern family to its dedicated renderers.
 */
public class MatchPatternRenderer implements Renderer<MatchPattern> {
    /**
     * Creates a match-pattern renderer.
     */
    public MatchPatternRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(MatchPattern node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        switch (node) {
            case MatchPattern.Variable value -> w.append(MatchPattern.Variable.class, value);
            case MatchPattern.Sequence value -> w.append(MatchPattern.Sequence.class, value);
            case MatchPattern.Alternation value -> w.append(MatchPattern.Alternation.class, value);
            case MatchPattern.Permutation value -> w.append(MatchPattern.Permutation.class, value);
            case MatchPattern.Anchor value -> w.append(MatchPattern.Anchor.class, value);
            case MatchPattern.Empty value -> w.append(MatchPattern.Empty.class, value);
            case MatchPattern.Exclusion value -> w.append(MatchPattern.Exclusion.class, value);
            case MatchPattern.Quantified value -> w.append(MatchPattern.Quantified.class, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern> targetType() {
        return MatchPattern.class;
    }
}
