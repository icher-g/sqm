package io.sqm.render.oracle;

import io.sqm.core.ResultClause;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle {@code UPDATE} statements with {@code RETURNING ... INTO}.
 */
public class UpdateStatementRenderer extends io.sqm.render.ansi.UpdateStatementRenderer {

    /**
     * Creates an Oracle update-statement renderer.
     */
    public UpdateStatementRenderer() {
    }

    @Override
    protected void renderReturning(ResultClause result, RenderContext ctx, SqlWriter w) {
        OracleResultRendering.renderReturningInto("UPDATE ... RETURNING INTO", result, ctx, w);
    }

    @Override
    protected void renderOutput(ResultClause result, RenderContext ctx, SqlWriter w) {
        // Oracle renders result clauses through RETURNING ... INTO.
    }
}
