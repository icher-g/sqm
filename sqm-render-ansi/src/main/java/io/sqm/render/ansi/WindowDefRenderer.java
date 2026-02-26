package io.sqm.render.ansi;

import io.sqm.core.WindowDef;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders WINDOW definitions.
 */
public class WindowDefRenderer implements Renderer<WindowDef> {
    /**
     * Creates a window-definition renderer.
     */
    public WindowDefRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(WindowDef node, RenderContext ctx, SqlWriter w) {
        w.append("WINDOW").space();
        w.append(renderIdentifier(node.name(), ctx.dialect().quoter())).space().append("AS").space();
        w.append(node.spec(), true, false);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<WindowDef> targetType() {
        return WindowDef.class;
    }
}
