package io.sqm.render.ansi;

import io.sqm.core.Assignment;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders baseline ANSI {@code column = value} assignments.
 */
public class AssignmentRenderer implements Renderer<Assignment> {

    /**
     * Creates an assignment renderer.
     */
    public AssignmentRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(Assignment node, RenderContext ctx, SqlWriter w) {
        w.append(renderQualifiedName(node.column(), ctx.dialect().quoter()))
            .space()
            .append("=")
            .space()
            .append(node.value());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<Assignment> targetType() {
        return Assignment.class;
    }
}
