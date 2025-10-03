package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Values;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

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
