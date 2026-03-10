package io.sqm.render.ansi;

import io.sqm.core.Assignment;
import io.sqm.core.Identifier;
import io.sqm.core.InsertSource;
import io.sqm.core.InsertStatement;
import io.sqm.core.Predicate;
import io.sqm.core.Query;
import io.sqm.core.RowValues;
import io.sqm.core.SelectItem;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

import java.util.List;

/**
 * Renders baseline ANSI {@code INSERT} statements.
 */
public class InsertStatementRenderer implements Renderer<InsertStatement> {

    /**
     * Creates an insert-statement renderer.
     */
    public InsertStatementRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(InsertStatement node, RenderContext ctx, SqlWriter w) {
        renderInsertPrefix(node, ctx, w);

        if (!node.columns().isEmpty()) {
            w.space().append("(");
            w.comma(node.columns(), ctx.dialect().quoter());
            w.append(")");
        }

        renderSource(node.source(), w);
        renderOnConflict(node.conflictTarget(),
            node.onConflictAction(),
            node.conflictUpdateAssignments(),
            node.conflictUpdateWhere(),
            ctx,
            w);
        renderReturning(node.returning(), ctx, w);
    }

    /**
     * Renders the leading insert keywords.
     *
     * @param node insert statement
     * @param ctx render context
     * @param w SQL writer
     */
    protected void renderInsertPrefix(InsertStatement node, RenderContext ctx, SqlWriter w) {
        if (node.insertMode() != InsertStatement.InsertMode.STANDARD) {
            throw new UnsupportedDialectFeatureException(unsupportedInsertModeName(node.insertMode()), ctx.dialect().name());
        }
        w.append("INSERT INTO").space().append(node.table());
    }

    /**
     * Renders optional {@code ON CONFLICT} clause.
     *
     * @param target conflict target
     * @param action conflict action
     * @param assignments conflict-update assignments
     * @param where conflict-update predicate
     * @param ctx render context
     * @param w SQL writer
     */
    protected void renderOnConflict(List<Identifier> target,
                                    InsertStatement.OnConflictAction action,
                                    List<Assignment> assignments,
                                    Predicate where,
                                    RenderContext ctx,
                                    SqlWriter w) {
        if (action != InsertStatement.OnConflictAction.NONE) {
            throw new UnsupportedDialectFeatureException("INSERT ... ON CONFLICT", ctx.dialect().name());
        }
    }

    /**
     * Renders optional {@code RETURNING} clause.
     *
     * @param returning returning projection items
     * @param ctx render context
     * @param w SQL writer
     */
    protected void renderReturning(List<SelectItem> returning, RenderContext ctx, SqlWriter w) {
        if (!returning.isEmpty()) {
            throw new UnsupportedDialectFeatureException("INSERT ... RETURNING", ctx.dialect().name());
        }
    }

    private void renderSource(InsertSource source, SqlWriter w) {
        if (source instanceof Query query) {
            w.space().append(query);
            return;
        }

        if (source instanceof RowValues rowValues) {
            w.space().append("VALUES").space();
            w.append(RowValues.class, rowValues);
            return;
        }

        throw new IllegalArgumentException("Unsupported INSERT source: " + source.getClass().getName());
    }

    private static String unsupportedInsertModeName(InsertStatement.InsertMode insertMode) {
        return switch (insertMode) {
            case STANDARD -> "INSERT INTO";
            case IGNORE -> "INSERT IGNORE";
            case REPLACE -> "REPLACE INTO";
        };
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<InsertStatement> targetType() {
        return InsertStatement.class;
    }
}
