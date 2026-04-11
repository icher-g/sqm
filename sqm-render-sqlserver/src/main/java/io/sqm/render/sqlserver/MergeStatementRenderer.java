package io.sqm.render.sqlserver;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeDoNothingAction;
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
        if (!node.hints().isEmpty()) {
            throw new UnsupportedDialectFeatureException("MERGE statement hints", ctx.dialect().name());
        }
        validate(node);

        w.append("MERGE");
        if (node.topSpec() != null) {
            SqlServerTopSpecRenderSupport.renderTopSpec(node.topSpec(), w);
        }
        w.space().append("INTO").space().append(node.target());
        w.newline().append("USING").space().append(node.source());
        w.newline().append("ON").space().append(node.on());
        for (var clause : node.clauses()) {
            w.newline().append(clause);
        }
        if (node.result() != null) {
            w.newline().append(node.result());
        }
    }

    private void validate(MergeStatement node) {
        if (node.topSpec() != null && node.topSpec().withTies()) {
            throw new UnsupportedOperationException("SQL Server MERGE TOP does not support WITH TIES");
        }
        if (node.clauses().stream().anyMatch(clause -> clause.action() instanceof MergeDoNothingAction)) {
            throw new UnsupportedOperationException("SQL Server MERGE DO NOTHING actions are not supported");
        }

        var matchedClauses = node.clauses().stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.MATCHED)
            .toList();
        if (matchedClauses.size() > 2) {
            throw new UnsupportedOperationException("SQL Server MERGE supports at most two WHEN MATCHED clauses");
        }

        if (matchedClauses.size() == 2) {
            validateDualUpdateDeleteClauses(matchedClauses, "WHEN MATCHED");
        }

        long notMatchedInsertCount = node.clauses().stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.NOT_MATCHED && clause.action() instanceof MergeInsertAction)
            .count();

        if (notMatchedInsertCount > 1) {
            throw new UnsupportedOperationException("SQL Server MERGE supports at most one WHEN NOT MATCHED THEN INSERT clause");
        }

        var notMatchedBySourceClauses = node.clauses().stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.NOT_MATCHED_BY_SOURCE)
            .toList();

        if (notMatchedBySourceClauses.size() > 2) {
            throw new UnsupportedOperationException("SQL Server MERGE supports at most two WHEN NOT MATCHED BY SOURCE clauses");
        }

        if (notMatchedBySourceClauses.size() == 2) {
            validateDualUpdateDeleteClauses(notMatchedBySourceClauses, "WHEN NOT MATCHED BY SOURCE");
        }
    }

    private void validateDualUpdateDeleteClauses(java.util.List<MergeClause> clauses, String label) {
        if (clauses.getFirst().condition() == null) {
            throw new UnsupportedOperationException("SQL Server MERGE requires the first " + label
                + " clause to include AND <search_condition> when two " + label + " clauses are present");
        }

        var firstAction = clauses.getFirst().action();
        var secondAction = clauses.get(1).action();

        if ((firstAction instanceof MergeUpdateAction && secondAction instanceof MergeUpdateAction)
            || (firstAction instanceof MergeDeleteAction && secondAction instanceof MergeDeleteAction)) {
            throw new UnsupportedOperationException("SQL Server MERGE requires one UPDATE and one DELETE action when two "
                + label + " clauses are present");
        }
    }
}
