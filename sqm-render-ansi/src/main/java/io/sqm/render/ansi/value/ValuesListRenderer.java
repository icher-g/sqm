package io.sqm.render.ansi.value;

import io.sqm.core.Values;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

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
