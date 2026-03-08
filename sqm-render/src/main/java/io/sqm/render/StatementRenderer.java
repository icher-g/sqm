package io.sqm.render;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Entry-point renderer for SQL statements.
 */
public class StatementRenderer implements Renderer<Statement> {

    /**
     * Creates a statement renderer.
     */
    public StatementRenderer() {
    }

    /**
     * Renders a statement by delegating to supported statement-family renderers.
     *
     * @param node statement node to render
     * @param ctx rendering context
     * @param w SQL writer
     */
    @Override
    public void render(Statement node, RenderContext ctx, SqlWriter w) {
        if (node instanceof Query query) {
            w.append(query);
            return;
        }
        if (node instanceof InsertStatement insertStatement) {
            w.append(insertStatement);
            return;
        }
        if (node instanceof UpdateStatement updateStatement) {
            w.append(updateStatement);
            return;
        }
        if (node instanceof DeleteStatement deleteStatement) {
            w.append(deleteStatement);
            return;
        }
        throw new UnsupportedOperationException("Statement type is not supported by this renderer yet: " + node.getClass().getSimpleName());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return statement root type
     */
    @Override
    public Class<Statement> targetType() {
        return Statement.class;
    }
}