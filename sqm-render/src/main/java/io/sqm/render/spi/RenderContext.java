package io.sqm.render.spi;

import io.sqm.core.Node;
import io.sqm.render.DefaultRenderContext;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.SqlText;
import io.sqm.render.SqlWriter;

public interface RenderContext {

    static RenderContext of(SqlDialect dialect) {
        return new DefaultRenderContext(dialect);
    }

    /**
     * Gets the concrete implementation of a SQL dialect.
     *
     * @return a SQL dialect.
     */
    SqlDialect dialect();

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param w    a writer.
     */
    default <T extends Node> void render(T node, SqlWriter w) {
        w.append(node);
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     */
    default <T extends Node> SqlText render(T node) {
        return render(node, RenderOptions.of(ParameterizationMode.Inline));
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     */
    default <T extends Node> SqlText render(T node, RenderOptions options) {
        var preparedNode = dialect().beforeRender(node, options);
        var w = new DefaultSqlWriter(this);
        w.append(preparedNode.node());
        return w.toText(preparedNode.params());
    }
}
