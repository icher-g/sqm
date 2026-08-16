package io.sqm.render.oracle;

import io.sqm.core.MergeDoNothingAction;
import io.sqm.core.MergeStatement;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders the baseline Oracle {@code MERGE} statement subset.
 */
public class MergeStatementRenderer extends io.sqm.render.ansi.MergeStatementRenderer {

    /**
     * Creates an Oracle merge-statement renderer.
     */
    public MergeStatementRenderer() {
    }

    @Override
    public void render(MergeStatement node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.MERGE_STATEMENT)) {
            throw new UnsupportedDialectFeatureException("MERGE", ctx.dialect().name());
        }
        if (node.topSpec() != null) {
            throw new UnsupportedDialectFeatureException("MERGE TOP", ctx.dialect().name());
        }
        if (node.result() != null) {
            throw new UnsupportedDialectFeatureException("MERGE RETURNING / OUTPUT", ctx.dialect().name());
        }
        if (node.clauses().stream().anyMatch(clause -> clause.action() instanceof MergeDoNothingAction)) {
            throw new UnsupportedOperationException("Oracle MERGE DO NOTHING actions are not supported");
        }

        w.append("MERGE");
        OracleHintRenderSupport.renderStatementHints(node.hints(), "MERGE optimizer hints", ctx, w);
        w.space().append("INTO").space().append(node.target());
        w.newline().append("USING").space().append(node.source());
        w.newline().append("ON").space().append("(").append(node.on()).append(")");
        for (var clause : node.clauses()) {
            w.newline().append(clause);
        }
    }
}
