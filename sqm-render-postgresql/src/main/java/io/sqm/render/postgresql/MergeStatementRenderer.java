package io.sqm.render.postgresql;

import io.sqm.core.MergeStatement;
import io.sqm.core.ResultClause;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders the first PostgreSQL {@code MERGE} slice.
 */
public class MergeStatementRenderer extends io.sqm.render.ansi.MergeStatementRenderer {

    /**
     * Creates a PostgreSQL merge-statement renderer.
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

        if (node.topSpec() != null) {
            throw new UnsupportedDialectFeatureException("MERGE TOP", ctx.dialect().name());
        }
        if (node.clauses().stream().anyMatch(clause -> clause.matchType() == io.sqm.core.MergeClause.MatchType.NOT_MATCHED_BY_SOURCE)
            && !ctx.dialect().capabilities().supports(SqlFeature.MERGE_NOT_MATCHED_BY_SOURCE_CLAUSE)) {
            throw new UnsupportedDialectFeatureException("MERGE WHEN NOT MATCHED BY SOURCE", ctx.dialect().name());
        }

        w.append("MERGE INTO").space().append(node.target());
        w.space().append("USING").space().append(node.source());
        w.space().append("ON").space().append(node.on());
        for (var clause : node.clauses()) {
            w.space().append(clause);
        }
        renderReturning(node.result(), ctx, w);
    }

    /**
     * Renders optional PostgreSQL {@code RETURNING} clause.
     *
     * @param result returning projection items
     * @param ctx render context
     * @param w SQL writer
     */
    protected void renderReturning(ResultClause result, RenderContext ctx, SqlWriter w) {
        if (result == null || result.items().isEmpty()) {
            return;
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.MERGE_RESULT_CLAUSE)) {
            throw new UnsupportedDialectFeatureException("MERGE ... RETURNING", ctx.dialect().name());
        }
        w.space().append("RETURNING").space().comma(result.items());
    }
}
