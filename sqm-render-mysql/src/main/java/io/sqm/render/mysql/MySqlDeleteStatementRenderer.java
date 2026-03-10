package io.sqm.render.mysql;

import io.sqm.core.Join;
import io.sqm.core.TableRef;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * Renders MySQL {@code DELETE} statements, including canonical {@code USING ... JOIN} forms.
 */
public class MySqlDeleteStatementRenderer extends io.sqm.render.ansi.DeleteStatementRenderer {

    /**
     * Creates a MySQL delete renderer.
     */
    public MySqlDeleteStatementRenderer() {
    }

    /**
     * Renders optional MySQL {@code USING} sources.
     *
     * @param using using sources
     * @param ctx render context
     * @param w SQL writer
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
     * @param ctx render context
     * @param w SQL writer
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
}