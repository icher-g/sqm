package io.sqm.render.ansi.value;

import io.sqm.core.Values;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

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
