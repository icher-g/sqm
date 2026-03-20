package io.sqm.render.sqlserver;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.MergeStatement;
import io.sqm.core.MergeUpdateAction;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders the first SQL Server {@code MERGE} slice.
 */
public class MergeStatementRenderer extends io.sqm.render.ansi.MergeStatementRenderer {

    /**
     * Creates a SQL Server merge-statement renderer.
     */
    public MergeStatementRenderer() {
    }

    @Override
    public void render(MergeStatement node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.MERGE_STATEMENT)) {
            throw new UnsupportedDialectFeatureException("MERGE", ctx.dialect().name());
        }
        validate(node);

        w.append("MERGE INTO").space().append(node.target());
        w.space().append("USING").space().append(node.source());
        w.space().append("ON").space().append(node.on());
        for (var clause : node.clauses()) {
            w.space().append(clause);
        }
        if (node.result() != null) {
            throw new UnsupportedOperationException("SQL Server MERGE OUTPUT is not supported yet");
        }
    }

    private void validate(MergeStatement node) {
        boolean matchedUpdate = false;
        boolean matchedDelete = false;
        boolean notMatchedInsert = false;

        for (MergeClause clause : node.clauses()) {
            if (clause.matchType() == MergeClause.MatchType.MATCHED && clause.action() instanceof MergeUpdateAction) {
                if (matchedUpdate) {
                    throw new UnsupportedOperationException("SQL Server MERGE supports at most one WHEN MATCHED THEN UPDATE clause in this slice");
                }
                matchedUpdate = true;
            }
            else if (clause.matchType() == MergeClause.MatchType.MATCHED && clause.action() instanceof MergeDeleteAction) {
                if (matchedDelete) {
                    throw new UnsupportedOperationException("SQL Server MERGE supports at most one WHEN MATCHED THEN DELETE clause in this slice");
                }
                matchedDelete = true;
            }
            else if (clause.matchType() == MergeClause.MatchType.NOT_MATCHED && clause.action() instanceof MergeInsertAction) {
                if (notMatchedInsert) {
                    throw new UnsupportedOperationException("SQL Server MERGE supports at most one WHEN NOT MATCHED THEN INSERT clause in this slice");
                }
                notMatchedInsert = true;
            }
        }
    }
}
