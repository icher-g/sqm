package io.sqm.render.ansi;

import io.sqm.core.SelectItem;
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
        w.append("UPDATE").space().append(node.table());
        w.space().append("SET").space();
        w.comma(node.assignments());

        renderFrom(node.from(), ctx, w);

        if (node.where() != null) {
            w.space().append("WHERE").space().append(node.where());
        }

        renderReturning(node.returning(), ctx, w);
    }

    /**
     * Renders optional {@code FROM} sources.
     *
     * @param from from sources
     * @param ctx render context
     * @param w SQL writer
     */
    protected void renderFrom(List<TableRef> from, RenderContext ctx, SqlWriter w) {
        if (!from.isEmpty()) {
            throw new UnsupportedDialectFeatureException("UPDATE ... FROM", ctx.dialect().name());
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
