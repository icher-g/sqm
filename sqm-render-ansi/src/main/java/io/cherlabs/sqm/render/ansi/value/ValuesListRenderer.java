package io.cherlabs.sqm.render.ansi.value;

import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.render.spi.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

public class ValuesListRenderer implements Renderer<Values.ListValues> {
    @Override
    public Class<Values.ListValues> targetType() {
        return Values.ListValues.class;
    }

    @Override
    public void render(Values.ListValues entity, RenderContext ctx, SqlWriter w) {
        w.append(ctx.bindOrFormat(entity.items()));
    }
}
