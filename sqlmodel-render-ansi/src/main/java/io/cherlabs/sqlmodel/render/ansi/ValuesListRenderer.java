package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Values;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class ValuesListRenderer implements Renderer<Values.ListValues> {
    @Override
    public void render(Values.ListValues entity, RenderContext ctx, SqlWriter w) {
        w.append(ctx.bindOrFormat(entity.items()));
    }
}
