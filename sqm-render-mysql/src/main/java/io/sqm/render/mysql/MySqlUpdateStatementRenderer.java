package io.sqm.render.mysql;

import io.sqm.core.Join;
import io.sqm.core.SelectItem;
import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * Renders MySQL {@code UPDATE} statements, including joined target forms.
 */
public class MySqlUpdateStatementRenderer extends io.sqm.render.ansi.UpdateStatementRenderer {

    /**
     * Creates a MySQL update renderer.
     */
    public MySqlUpdateStatementRenderer() {
    }

    /**
     * Renders MySQL-specific tokens after {@code UPDATE}.
     *
     * @param node update statement
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    protected void renderAfterUpdateKeyword(UpdateStatement node, RenderContext ctx, SqlWriter w) {
        if (!node.optimizerHints().isEmpty() && !ctx.dialect().capabilities().supports(SqlFeature.OPTIMIZER_HINT_COMMENT)) {
            throw new UnsupportedDialectFeatureException("UPDATE optimizer hints", ctx.dialect().name());
        }

        for (var hint : node.optimizerHints()) {
            w.space().append("/*+ ").append(hint).append(" */");
        }
    }

    /**
     * Renders optional joined sources attached to the target table.
     *
     * @param joins joined sources
     * @param ctx render context
     * @param w SQL writer
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
     * Renders optional MySQL {@code RETURNING} clause.
     *
     * @param returning returning projection items
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    protected void renderReturning(List<SelectItem> returning, RenderContext ctx, SqlWriter w) {
        if (returning.isEmpty()) {
            return;
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.DML_RETURNING)) {
            throw new UnsupportedDialectFeatureException("UPDATE ... RETURNING", ctx.dialect().name());
        }
        w.space().append("RETURNING").space().comma(returning);
    }
}
