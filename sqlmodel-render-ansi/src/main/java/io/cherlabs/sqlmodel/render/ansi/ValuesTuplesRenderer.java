package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Values;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class ValuesTuplesRenderer implements Renderer<Values.Tuples> {
    @Override
    public void render(Values.Tuples entity, RenderContext ctx, SqlWriter w) {
        w.append(ctx.bindOrFormat(entity.rows()));
    }
}
