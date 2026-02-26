package io.sqm.render.spi;

import io.sqm.core.Node;
import io.sqm.render.defaults.DefaultRenderContext;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.SqlText;
import io.sqm.render.SqlWriter;

/**
 * Rendering context coordinating dialect and writer interactions.
 */
public interface RenderContext {

    /**
     * Creates a render context for the provided dialect.
     *
     * @param dialect SQL dialect used for rendering.
     * @return a render context.
     */
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
        * @param <T>  node type.
     * @param node a node to render.
     * @param w    a writer.
     */
    default <T extends Node> void render(T node, SqlWriter w) {
        w.append(node);
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
        * @param <T>  node type.
     * @param node a node to render.
        * @return rendered SQL text.
     */
    default <T extends Node> SqlText render(T node) {
        return render(node, RenderOptions.of(ParameterizationMode.Inline));
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
        * @param <T>     node type.
     * @param node a node to render.
        * @param options render options.
        * @return rendered SQL text.
     */
    default <T extends Node> SqlText render(T node, RenderOptions options) {
        var preparedNode = dialect().beforeRender(node, options);
        var w = new DefaultSqlWriter(this);
        w.append(preparedNode.node());
        return w.toText(preparedNode.params());
    }
}
