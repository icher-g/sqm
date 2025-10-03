package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.FunctionColumn;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class StarArgRenderer implements Renderer<FunctionColumn.Arg.Star> {
    @Override
    public Class<FunctionColumn.Arg.Star> targetType() {
        return FunctionColumn.Arg.Star.class;
    }

    @Override
    public void render(FunctionColumn.Arg.Star entity, RenderContext ctx, SqlWriter w) {
        w.append("*");
    }
}
