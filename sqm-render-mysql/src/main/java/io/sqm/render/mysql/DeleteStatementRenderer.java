package io.sqm.render.mysql;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Join;
import io.sqm.core.ResultClause;
import io.sqm.core.TableRef;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * Renders MySQL {@code DELETE} statements, including canonical {@code USING ... JOIN} forms.
 */
public class DeleteStatementRenderer extends io.sqm.render.ansi.DeleteStatementRenderer {

    /**
     * Creates a MySQL delete renderer.
     */
    public DeleteStatementRenderer() {
    }

    /**
     * Renders MySQL-specific tokens after {@code DELETE}.
     *
     * @param node delete statement
     * @param ctx  render context
     * @param w    SQL writer
     */
    @Override
    protected void renderAfterDeleteKeyword(DeleteStatement node, RenderContext ctx, SqlWriter w) {
        MySqlHintRenderSupport.renderStatementHints(node.hints(), "DELETE optimizer hints", ctx, w);
    }

    /**
     * Renders optional MySQL {@code USING} sources.
     *
     * @param using using sources
     * @param ctx   render context
     * @param w     SQL writer
     */
    @Override
    protected void renderUsing(List<TableRef> using, RenderContext ctx, SqlWriter w) {
        if (using.isEmpty()) {
            return;
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.DELETE_USING_JOIN)) {
            throw new UnsupportedDialectFeatureException("DELETE ... USING", ctx.dialect().name());
        }
        w.space().append("USING").space().comma(using);
    }

    /**
     * Renders optional joined sources attached to the {@code USING} clause.
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
        if (!ctx.dialect().capabilities().supports(SqlFeature.DELETE_USING_JOIN)) {
            throw new UnsupportedDialectFeatureException("DELETE ... USING ... JOIN", ctx.dialect().name());
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
            throw new UnsupportedDialectFeatureException("DELETE ... RETURNING", ctx.dialect().name());
        }
        w.space().append("RETURNING").space().comma(result.items());
    }

    @Override
    protected void renderOutput(ResultClause result, RenderContext ctx, SqlWriter w) {
        // noop
    }
}
