package io.sqm.render.ansi;

import io.sqm.core.RowsPerMatch;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders the row-count and empty-match mode.
 */
public class RowsPerMatchRenderer implements Renderer<RowsPerMatch> {
    /**
     * Creates a rows-per-match renderer.
     */
    public RowsPerMatchRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(RowsPerMatch node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        if (node.mode() == RowsPerMatch.Mode.ONE) {
            w.append("ONE ROW PER MATCH");
            return;
        }
        w.append("ALL ROWS PER MATCH");
        switch (node.emptyMatchHandling()) {
            case DEFAULT -> {
            }
            case SHOW_EMPTY -> w.space().append("SHOW EMPTY MATCHES");
            case OMIT_EMPTY -> w.space().append("OMIT EMPTY MATCHES");
            case WITH_UNMATCHED -> w.space().append("WITH UNMATCHED ROWS");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<RowsPerMatch> targetType() {
        return RowsPerMatch.class;
    }
}
