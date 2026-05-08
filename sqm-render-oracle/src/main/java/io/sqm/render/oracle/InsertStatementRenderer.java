package io.sqm.render.oracle;

import io.sqm.core.ResultClause;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle {@code INSERT} statements with {@code RETURNING ... INTO}.
 */
public class InsertStatementRenderer extends io.sqm.render.ansi.InsertStatementRenderer {

    /**
     * Creates an Oracle insert-statement renderer.
     */
    public InsertStatementRenderer() {
    }

    @Override
    protected void renderReturning(ResultClause result, RenderContext ctx, SqlWriter w) {
        OracleResultRendering.renderReturningInto("INSERT ... RETURNING INTO", result, ctx, w);
    }

    @Override
    protected void renderOutput(ResultClause result, RenderContext ctx, SqlWriter w) {
        // Oracle renders result clauses through RETURNING ... INTO.
    }
}
