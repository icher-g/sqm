package io.sqm.render.oracle;

import io.sqm.core.ResultClause;
import io.sqm.core.DeleteStatement;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle {@code DELETE} statements with {@code RETURNING ... INTO}.
 */
public class DeleteStatementRenderer extends io.sqm.render.ansi.DeleteStatementRenderer {

    /**
     * Creates an Oracle delete-statement renderer.
     */
    public DeleteStatementRenderer() {
    }

    @Override
    protected void renderAfterDeleteKeyword(DeleteStatement node, RenderContext ctx, SqlWriter w) {
        OracleHintRenderSupport.renderStatementHints(node.hints(), "DELETE optimizer hints", ctx, w);
    }

    @Override
    protected void renderReturning(ResultClause result, RenderContext ctx, SqlWriter w) {
        OracleResultRendering.renderReturningInto("DELETE ... RETURNING INTO", result, ctx, w);
    }

    @Override
    protected void renderOutput(ResultClause result, RenderContext ctx, SqlWriter w) {
        // Oracle renders result clauses through RETURNING ... INTO.
    }
}
