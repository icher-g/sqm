package io.sqm.render.mysql;

import io.sqm.core.*;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * Renders MySQL {@code INSERT} statements.
 */
public class InsertStatementRenderer extends io.sqm.render.ansi.InsertStatementRenderer {

    /**
     * Creates a MySQL insert renderer.
     */
    public InsertStatementRenderer() {
    }

    /**
     * Renders the leading MySQL insert keywords.
     *
     * @param node insert statement
     * @param ctx  render context
     * @param w    SQL writer
     */
    @Override
    protected void renderInsertPrefix(InsertStatement node, RenderContext ctx, SqlWriter w) {
        switch (node.insertMode()) {
            case STANDARD -> w.append("INSERT INTO").space().append(node.table());
            case IGNORE -> {
                if (!ctx.dialect().capabilities().supports(SqlFeature.INSERT_IGNORE)) {
                    throw new UnsupportedDialectFeatureException("INSERT IGNORE", ctx.dialect().name());
                }
                w.append("INSERT IGNORE INTO").space().append(node.table());
            }
            case REPLACE -> {
                if (!ctx.dialect().capabilities().supports(SqlFeature.REPLACE_INTO)) {
                    throw new UnsupportedDialectFeatureException("REPLACE INTO", ctx.dialect().name());
                }
                w.append("REPLACE INTO").space().append(node.table());
            }
        }
    }

    /**
     * Renders optional MySQL {@code ON DUPLICATE KEY UPDATE} clause.
     *
     * @param target      conflict target
     * @param action      conflict action
     * @param assignments conflict-update assignments
     * @param where       conflict-update predicate
     * @param ctx         render context
     * @param w           SQL writer
     */
    @Override
    protected void renderOnConflict(List<Identifier> target,
        InsertStatement.OnConflictAction action,
        List<Assignment> assignments,
        Predicate where,
        RenderContext ctx,
        SqlWriter w) {
        if (action == InsertStatement.OnConflictAction.NONE) {
            return;
        }
        if (action != InsertStatement.OnConflictAction.DO_UPDATE || !target.isEmpty() || where != null) {
            throw new UnsupportedDialectFeatureException("INSERT ... ON CONFLICT", ctx.dialect().name());
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.INSERT_ON_DUPLICATE_KEY_UPDATE)) {
            throw new UnsupportedDialectFeatureException("INSERT ... ON DUPLICATE KEY UPDATE", ctx.dialect().name());
        }
        w.space().append("ON DUPLICATE KEY UPDATE").space().comma(assignments);
    }

    /**
     * Renders optional {@code RETURNING} clause.
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
            throw new UnsupportedDialectFeatureException("INSERT ... RETURNING", ctx.dialect().name());
        }
        w.space().append("RETURNING").space().comma(result.items());
    }

    @Override
    protected void renderOutput(ResultClause result, RenderContext ctx, SqlWriter w) {
        // noop
    }
}
