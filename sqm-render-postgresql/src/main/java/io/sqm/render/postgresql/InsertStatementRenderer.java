package io.sqm.render.postgresql;

import io.sqm.core.Assignment;
import io.sqm.core.Identifier;
import io.sqm.core.InsertStatement;
import io.sqm.core.Predicate;
import io.sqm.core.SelectItem;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * Renders PostgreSQL {@code INSERT} statements, including optional {@code ON CONFLICT} and {@code RETURNING}.
 */
public class InsertStatementRenderer extends io.sqm.render.ansi.InsertStatementRenderer {

    /**
     * Creates a PostgreSQL insert-statement renderer.
     */
    public InsertStatementRenderer() {
    }

    /**
     * Renders optional PostgreSQL {@code ON CONFLICT} clause.
     *
     * @param target conflict target
     * @param action conflict action
     * @param assignments conflict-update assignments
     * @param where conflict-update predicate
     * @param ctx render context
     * @param w SQL writer
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

        if (!ctx.dialect().capabilities().supports(SqlFeature.INSERT_ON_CONFLICT)) {
            throw new UnsupportedDialectFeatureException("INSERT ... ON CONFLICT", ctx.dialect().name());
        }

        w.space().append("ON CONFLICT");

        if (!target.isEmpty()) {
            w.space().append("(");
            w.comma(target, ctx.dialect().quoter());
            w.append(")");
        }

        if (action == InsertStatement.OnConflictAction.DO_NOTHING) {
            w.space().append("DO NOTHING");
            return;
        }

        w.space().append("DO UPDATE SET").space().comma(assignments);
        if (where != null) {
            w.space().append("WHERE").space().append(where);
        }
    }

    /**
     * Renders optional PostgreSQL {@code RETURNING} clause.
     *
     * @param returning returning projection items
     * @param ctx       render context
     * @param w         SQL writer
     */
    @Override
    protected void renderReturning(List<SelectItem> returning, RenderContext ctx, SqlWriter w) {
        if (returning.isEmpty()) {
            return;
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.DML_RETURNING)) {
            throw new UnsupportedDialectFeatureException("INSERT ... RETURNING", ctx.dialect().name());
        }
        w.space().append("RETURNING").space().comma(returning);
    }
}
