package io.sqm.render.ansi;

import io.sqm.core.InsertSource;
import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
import io.sqm.core.RowValues;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

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
        w.append("INSERT INTO").space().append(node.table());

        if (!node.columns().isEmpty()) {
            w.space().append("(");
            w.comma(node.columns(), ctx.dialect().quoter());
            w.append(")");
        }

        renderSource(node.source(), w);
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

