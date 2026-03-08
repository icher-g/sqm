package io.sqm.render;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.WithQuery;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Entry-point renderer for SQL query nodes.
 */
public class QueryRenderer implements Renderer<Query> {

    /**
     * Creates a query renderer.
     */
    public QueryRenderer() {
    }

    /**
     * Renders a query by delegating to the concrete query renderer.
     *
     * @param node query node to render
     * @param ctx rendering context
     * @param w SQL writer
     */
    @Override
    public void render(Query node, RenderContext ctx, SqlWriter w) {
        if (node instanceof WithQuery withQuery) {
            w.append(withQuery);
            return;
        }
        if (node instanceof CompositeQuery compositeQuery) {
            w.append(compositeQuery);
            return;
        }
        if (node instanceof SelectQuery selectQuery) {
            w.append(selectQuery);
            return;
        }
        throw new IllegalArgumentException("Unsupported query type: " + node.getClass().getName());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return query root type
     */
    @Override
    public Class<Query> targetType() {
        return Query.class;
    }
}
