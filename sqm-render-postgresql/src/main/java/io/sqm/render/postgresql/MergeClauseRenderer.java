package io.sqm.render.postgresql;

import io.sqm.core.MergeClause;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders PostgreSQL {@link MergeClause} nodes for the first shared slice.
 */
public class MergeClauseRenderer extends io.sqm.render.ansi.MergeClauseRenderer {

    /**
     * Creates a PostgreSQL merge-clause renderer.
     */
    public MergeClauseRenderer() {
    }

    @Override
    public void render(MergeClause node, RenderContext ctx, SqlWriter w) {
        if (node.matchType() == MergeClause.MatchType.NOT_MATCHED_BY_SOURCE
            && !ctx.dialect().capabilities().supports(SqlFeature.MERGE_NOT_MATCHED_BY_SOURCE_CLAUSE)) {
            throw new UnsupportedDialectFeatureException("MERGE WHEN NOT MATCHED BY SOURCE", ctx.dialect().name());
        }
        renderSupportedClause(node, w);
    }
}
