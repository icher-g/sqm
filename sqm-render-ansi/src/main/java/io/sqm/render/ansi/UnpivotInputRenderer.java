package io.sqm.render.ansi;

import io.sqm.core.UnpivotInput;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders unpivot input column groups.
 */
public class UnpivotInputRenderer implements Renderer<UnpivotInput> {
    /**
     * Creates an unpivot input renderer.
     */
    public UnpivotInputRenderer() {
    }

    /**
     * Renders the node into a SQL writer.
     *
     * @param node unpivot input
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(UnpivotInput node, RenderContext ctx, SqlWriter w) {
        var quoter = ctx.dialect().quoter();
        if (node.sourceColumns().size() == 1) {
            w.append(renderIdentifier(node.sourceColumns().getFirst(), quoter));
        }
        else {
            w.append("(").comma(node.sourceColumns(), quoter).append(")");
        }
        w.space().append("AS").space().append(node.label());
    }

    /**
     * Gets the renderer target type.
     *
     * @return unpivot input type
     */
    @Override
    public Class<UnpivotInput> targetType() {
        return UnpivotInput.class;
    }
}
