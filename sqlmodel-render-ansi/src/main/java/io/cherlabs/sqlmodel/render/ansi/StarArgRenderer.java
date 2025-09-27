package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.FunctionColumn;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class StarArgRenderer implements Renderer<FunctionColumn.Arg.Star> {
    @Override
    public void render(FunctionColumn.Arg.Star entity, RenderContext ctx, SqlWriter w) {
        w.append("*");
    }
}
