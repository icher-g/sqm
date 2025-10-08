package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

public class ValuesTuplesRenderer implements Renderer<Values.Tuples> {
    @Override
    public Class<Values.Tuples> targetType() {
        return Values.Tuples.class;
    }

    @Override
    public void render(Values.Tuples entity, RenderContext ctx, SqlWriter w) {
        w.append(ctx.bindOrFormat(entity.rows()));
    }
}
