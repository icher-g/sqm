package io.sqm.render.ansi;

import io.sqm.core.Join;
import io.sqm.core.ResultClause;
import io.sqm.core.TableRef;
import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

import java.util.List;

/**
 * Renders baseline ANSI {@code UPDATE} statements.
 */
public class UpdateStatementRenderer implements Renderer<UpdateStatement> {

    /**
     * Creates an update-statement renderer.
     */
    public UpdateStatementRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(UpdateStatement node, RenderContext ctx, SqlWriter w) {
        w.append("UPDATE");

        renderAfterUpdateKeyword(node, ctx, w);

        w.space().append(node.table());

        renderJoins(node.joins(), ctx, w);

        w.space().append("SET").space();
        w.comma(node.assignments());

        renderOutput(node.result(), ctx, w);
        renderFrom(node.from(), ctx, w);

        if (node.where() != null) {
            w.space().append("WHERE").space().append(node.where());
        }

        renderReturning(node.result(), ctx, w);
    }

    /**
     * Renders optional joined sources attached to the target table.
     *
     * @param joins joined sources
     * @param ctx   render context
     * @param w     SQL writer
     */
    protected void renderJoins(List<Join> joins, RenderContext ctx, SqlWriter w) {
        if (!joins.isEmpty()) {
            throw new UnsupportedDialectFeatureException("UPDATE ... JOIN", ctx.dialect().name());
        }
    }

    /**
     * Renders tokens that may appear after {@code UPDATE} and before the target table.
     *
     * @param node update statement
     * @param ctx  render context
     * @param w    SQL writer
     */
    protected void renderAfterUpdateKeyword(UpdateStatement node, RenderContext ctx, SqlWriter w) {
        if (!node.optimizerHints().isEmpty()) {
            throw new UnsupportedDialectFeatureException("UPDATE optimizer hints", ctx.dialect().name());
        }
    }

    /**
     * Renders optional {@code FROM} sources.
     *
     * @param from from sources
     * @param ctx  render context
     * @param w    SQL writer
     */
    protected void renderFrom(List<TableRef> from, RenderContext ctx, SqlWriter w) {
        if (!from.isEmpty()) {
            throw new UnsupportedDialectFeatureException("UPDATE ... FROM", ctx.dialect().name());
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
            throw new UnsupportedDialectFeatureException("UPDATE ... OUTPUT", ctx.dialect().name());
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
            throw new UnsupportedDialectFeatureException("UPDATE ... RETURNING", ctx.dialect().name());
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<UpdateStatement> targetType() {
        return UpdateStatement.class;
    }
}
