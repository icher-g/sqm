package io.sqm.render.ansi;

import io.sqm.core.DeleteStatement;
import io.sqm.core.SelectItem;
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
        w.append("DELETE FROM").space().append(node.table());

        renderUsing(node.using(), ctx, w);

        if (node.where() != null) {
            w.space().append("WHERE").space().append(node.where());
        }

        renderReturning(node.returning(), ctx, w);
    }

    /**
     * Renders optional {@code USING} sources.
     *
     * @param using using sources
     * @param ctx render context
     * @param w SQL writer
     */
    protected void renderUsing(List<TableRef> using, RenderContext ctx, SqlWriter w) {
        if (!using.isEmpty()) {
            throw new UnsupportedDialectFeatureException("DELETE ... USING", ctx.dialect().name());
        }
    }

    /**
     * Renders optional {@code RETURNING} projection items.
     *
     * @param returning returning projection items
     * @param ctx render context
     * @param w SQL writer
     */
    protected void renderReturning(List<SelectItem> returning, RenderContext ctx, SqlWriter w) {
        if (!returning.isEmpty()) {
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
