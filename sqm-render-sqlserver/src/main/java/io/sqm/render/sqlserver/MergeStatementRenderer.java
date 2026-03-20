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
        var matchedClauses = node.clauses().stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.MATCHED)
            .toList();
        if (matchedClauses.size() > 2) {
            throw new UnsupportedOperationException("SQL Server MERGE supports at most two WHEN MATCHED clauses");
        }

        if (matchedClauses.size() == 2) {
            if (matchedClauses.getFirst().condition() == null) {
                throw new UnsupportedOperationException("SQL Server MERGE requires the first WHEN MATCHED clause to include AND <search_condition> when two MATCHED clauses are present");
            }
            var firstAction = matchedClauses.getFirst().action();
            var secondAction = matchedClauses.get(1).action();
            if ((firstAction instanceof MergeUpdateAction && secondAction instanceof MergeUpdateAction)
                || (firstAction instanceof MergeDeleteAction && secondAction instanceof MergeDeleteAction)) {
                throw new UnsupportedOperationException("SQL Server MERGE requires one UPDATE and one DELETE action when two WHEN MATCHED clauses are present");
            }
        }

        long notMatchedInsertCount = node.clauses().stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.NOT_MATCHED && clause.action() instanceof MergeInsertAction)
            .count();
        if (notMatchedInsertCount > 1) {
            throw new UnsupportedOperationException("SQL Server MERGE supports at most one WHEN NOT MATCHED THEN INSERT clause");
        }
    }
}
