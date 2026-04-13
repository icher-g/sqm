package io.sqm.render.ansi;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Join;
import io.sqm.core.ResultClause;
import io.sqm.core.TableRef;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

import java.util.List;

/**
 * Renders baseline ANSI {@code DELETE} statements.
 */
public class DeleteStatementRenderer implements Renderer<DeleteStatement> {

    /**
     * Creates a delete-statement renderer.
     */
    public DeleteStatementRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(DeleteStatement node, RenderContext ctx, SqlWriter w) {
        w.append("DELETE");

        renderAfterDeleteKeyword(node, ctx, w);

        w.space().append("FROM").space().append(node.table());

        renderUsing(node.using(), ctx, w);
        renderJoins(node.joins(), ctx, w);
        renderOutput(node.result(), ctx, w);

        if (node.where() != null) {
            w.newline().append("WHERE").space().append(node.where());
        }

        renderReturning(node.result(), ctx, w);
    }

    /**
     * Renders optional {@code USING} sources.
     *
     * @param using using sources
     * @param ctx   render context
     * @param w     SQL writer
     */
    protected void renderUsing(List<TableRef> using, RenderContext ctx, SqlWriter w) {
        if (!using.isEmpty()) {
            throw new UnsupportedDialectFeatureException("DELETE ... USING", ctx.dialect().name());
        }
    }

    /**
     * Renders tokens that may appear after {@code DELETE} and before {@code FROM}.
     *
     * @param node delete statement
     * @param ctx  render context
     * @param w    SQL writer
     */
    protected void renderAfterDeleteKeyword(DeleteStatement node, RenderContext ctx, SqlWriter w) {
        if (!node.hints().isEmpty()) {
            throw new UnsupportedDialectFeatureException("DELETE optimizer hints", ctx.dialect().name());
        }
    }

    /**
     * Renders optional joined sources attached to the {@code USING} clause.
     *
     * @param joins joined sources
     * @param ctx   render context
     * @param w     SQL writer
     */
    protected void renderJoins(List<Join> joins, RenderContext ctx, SqlWriter w) {
        if (!joins.isEmpty()) {
            throw new UnsupportedDialectFeatureException("DELETE ... JOIN", ctx.dialect().name());
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
            throw new UnsupportedDialectFeatureException("DELETE ... OUTPUT", ctx.dialect().name());
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
            throw new UnsupportedDialectFeatureException("DELETE ... RETURNING", ctx.dialect().name());
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<DeleteStatement> targetType() {
        return DeleteStatement.class;
    }
}
