package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

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
