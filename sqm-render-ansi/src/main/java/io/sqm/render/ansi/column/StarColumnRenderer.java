package io.sqm.render.ansi.column;

import io.sqm.core.StarColumn;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

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
