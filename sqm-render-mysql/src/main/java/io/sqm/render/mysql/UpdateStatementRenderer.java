package io.sqm.render.mysql;

import io.sqm.core.Join;
import io.sqm.core.ResultClause;
import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * Renders MySQL {@code UPDATE} statements, including joined target forms.
 */
public class UpdateStatementRenderer extends io.sqm.render.ansi.UpdateStatementRenderer {

    /**
     * Creates a MySQL update renderer.
     */
    public UpdateStatementRenderer() {
    }

    /**
     * Renders MySQL-specific tokens after {@code UPDATE}.
     *
     * @param node update statement
     * @param ctx  render context
     * @param w    SQL writer
     */
    @Override
    protected void renderAfterUpdateKeyword(UpdateStatement node, RenderContext ctx, SqlWriter w) {
        MySqlHintRenderSupport.renderStatementHints(node.hints(), "UPDATE optimizer hints", ctx, w);
    }

    /**
     * Renders optional joined sources attached to the target table.
     *
     * @param joins joined sources
     * @param ctx   render context
     * @param w     SQL writer
     */
    @Override
    protected void renderJoins(List<Join> joins, RenderContext ctx, SqlWriter w) {
        if (joins.isEmpty()) {
            return;
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.UPDATE_JOIN)) {
            throw new UnsupportedDialectFeatureException("UPDATE ... JOIN", ctx.dialect().name());
        }
        for (var join : joins) {
            w.space().append(join);
        }
    }

    /**
     * Renders optional {@code OUTPUT} clause.
     *
     * @param result result clause
     * @param ctx    render context
     * @param w      SQL writer
     */
    @Override
    protected void renderReturning(ResultClause result, RenderContext ctx, SqlWriter w) {
        if (result == null || result.items().isEmpty()) {
            return;
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.DML_RESULT_CLAUSE)) {
            throw new UnsupportedDialectFeatureException("UPDATE ... RETURNING", ctx.dialect().name());
        }
        w.space().append("RETURNING").space().comma(result.items());
    }

    @Override
    protected void renderOutput(ResultClause result, RenderContext ctx, SqlWriter w) {
        // noop
    }
}
