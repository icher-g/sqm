package io.sqm.render.ansi.value;

import io.sqm.core.Values;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

public class ValuesSingleRenderer implements Renderer<Values.Single> {
    @Override
    public Class<Values.Single> targetType() {
        return Values.Single.class;
    }

    @Override
    public void render(Values.Single entity, RenderContext ctx, SqlWriter w) {
        w.append(ctx.bindOrFormat(entity.value()));
    }
}
