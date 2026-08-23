package io.sqm.render.oracle;

import io.sqm.core.PatternRecognitionTable;
import io.sqm.core.RowsPerMatch;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle row-pattern relations after checking Oracle-only option limits.
 */
public final class PatternRecognitionTableRenderer extends io.sqm.render.ansi.PatternRecognitionTableRenderer {
    /**
     * Creates an Oracle row-pattern relation renderer.
     */
    public PatternRecognitionTableRenderer() {
    }

    /** {@inheritDoc} */
    @Override
    public void render(PatternRecognitionTable node, RenderContext ctx, SqlWriter writer) {
        if (node.rowsPerMatch().emptyMatchHandling() != RowsPerMatch.EmptyMatchHandling.DEFAULT) {
            throw new UnsupportedDialectFeatureException(
                "MATCH_RECOGNIZE explicit empty or unmatched-row handling",
                ctx.dialect().name()
            );
        }
        super.render(node, ctx, writer);
    }
}
