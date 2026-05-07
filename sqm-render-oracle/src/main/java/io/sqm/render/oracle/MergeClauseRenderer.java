package io.sqm.render.oracle;

import io.sqm.core.MergeClause;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle {@link MergeClause} nodes.
 */
public class MergeClauseRenderer extends io.sqm.render.ansi.MergeClauseRenderer {

    /**
     * Creates an Oracle merge-clause renderer.
     */
    public MergeClauseRenderer() {
    }

    @Override
    public void render(MergeClause node, RenderContext ctx, SqlWriter w) {
        if (node.matchType() == MergeClause.MatchType.NOT_MATCHED_BY_SOURCE) {
            throw new UnsupportedDialectFeatureException("MERGE WHEN NOT MATCHED BY SOURCE", ctx.dialect().name());
        }
        renderSupportedClause(node, w);
    }
}
