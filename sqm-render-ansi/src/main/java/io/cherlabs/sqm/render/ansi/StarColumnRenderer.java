package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.StarColumn;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

/**
 * A renderer for a *.
 */
public class StarColumnRenderer implements Renderer<StarColumn> {
    @Override
    public void render(StarColumn entity, RenderContext ctx, SqlWriter w) {
        w.append("*");
    }

    @Override
    public Class<StarColumn> targetType() {
        return StarColumn.class;
    }
}
