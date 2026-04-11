package io.sqm.render.ansi;

import io.sqm.core.*;
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

    private static String unsupportedInsertModeName(InsertStatement.InsertMode insertMode) {
        return switch (insertMode) {
            case STANDARD -> "INSERT INTO";
            case IGNORE -> "INSERT IGNORE";
            case REPLACE -> "REPLACE INTO";
        };
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
        renderInsertKeyword(node, ctx, w);
        renderInsertHints(node, ctx, w);
        renderIntoKeyword(node, ctx, w);
        renderInsertTarget(node, ctx, w);

        if (!node.columns().isEmpty()) {
            w.space().append("(");
            w.comma(node.columns(), ctx.dialect().quoter());
            w.append(")");
        }

        renderOutput(node.result(), ctx, w);
        renderSource(node.source(), w);
        renderOnConflict(node.conflictTarget(),
            node.onConflictAction(),
            node.conflictUpdateAssignments(),
            node.conflictUpdateWhere(),
            ctx,
            w);
        renderReturning(node.result(), ctx, w);
    }

    /**
     * Renders the leading insert keywords.
     *
     * @param node insert statement
     * @param ctx  render context
     * @param w    SQL writer
     */
    protected void renderInsertKeyword(InsertStatement node, RenderContext ctx, SqlWriter w) {
        if (node.insertMode() != InsertStatement.InsertMode.STANDARD) {
            throw new UnsupportedDialectFeatureException(unsupportedInsertModeName(node.insertMode()), ctx.dialect().name());
        }
        w.append("INSERT");
    }

    /**
     * Renders optional insert-attached hints that appear between the insert keyword and {@code INTO}.
     *
     * @param node insert statement
     * @param ctx  render context
     * @param w    SQL writer
     */
    protected void renderInsertHints(InsertStatement node, RenderContext ctx, SqlWriter w) {
        if (!node.hints().isEmpty()) {
            throw new UnsupportedDialectFeatureException("INSERT statement hints", ctx.dialect().name());
        }
    }

    /**
     * Renders the {@code INTO} keyword.
     *
     * @param node insert statement
     * @param ctx  render context
     * @param w    SQL writer
     */
    @SuppressWarnings("unused")
    protected void renderIntoKeyword(InsertStatement node, RenderContext ctx, SqlWriter w) {
        w.space().append("INTO");
    }

    /**
     * Renders the insert target table.
     *
     * @param node insert statement
     * @param ctx  render context
     * @param w    SQL writer
     */
    @SuppressWarnings("unused")
    protected void renderInsertTarget(InsertStatement node, RenderContext ctx, SqlWriter w) {
        w.space().append(node.table());
    }

    /**
     * Renders optional {@code ON CONFLICT} clause.
     *
     * @param target      conflict target
     * @param action      conflict action
     * @param assignments conflict-update assignments
     * @param where       conflict-update predicate
     * @param ctx         render context
     * @param w           SQL writer
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
     * Renders optional {@code OUTPUT} clause.
     *
     * @param result result clause
     * @param ctx    render context
     * @param w      SQL writer
     */
    protected void renderOutput(ResultClause result, RenderContext ctx, SqlWriter w) {
        if (result != null) {
            throw new UnsupportedDialectFeatureException("INSERT ... OUTPUT", ctx.dialect().name());
        }
    }

    /**
     * Renders optional {@code RETURNING} clause.
     *
     * @param result result clause
     * @param ctx    render context
     * @param w      SQL writer
     */
    protected void renderReturning(ResultClause result, RenderContext ctx, SqlWriter w) {
        if (result != null) {
            throw new UnsupportedDialectFeatureException("INSERT ... RETURNING", ctx.dialect().name());
        }
    }

    private void renderSource(InsertSource source, SqlWriter w) {
        if (source instanceof Query query) {
            w.newline().append(query);
            return;
        }

        if (source instanceof RowValues rowValues) {
            w.newline().append("VALUES").space();
            w.append(RowValues.class, rowValues);
            return;
        }

        throw new IllegalArgumentException("Unsupported INSERT source: " + source.getClass().getName());
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
