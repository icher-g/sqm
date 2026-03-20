package io.sqm.render.sqlserver;

import io.sqm.core.MergeClause;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server {@link MergeClause} nodes.
 */
public class MergeClauseRenderer extends io.sqm.render.ansi.MergeClauseRenderer {

    /**
     * Creates a SQL Server merge-clause renderer.
     */
    public MergeClauseRenderer() {
    }

    @Override
    public void render(MergeClause node, RenderContext ctx, SqlWriter w) {
        w.append("WHEN");
        if (node.matchType() == MergeClause.MatchType.NOT_MATCHED) {
            w.space().append("NOT");
        }
        w.space().append("MATCHED");
        w.space().append("THEN").space().append(node.action());
    }
}
