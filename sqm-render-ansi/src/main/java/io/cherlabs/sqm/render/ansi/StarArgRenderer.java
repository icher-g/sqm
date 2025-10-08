package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.FunctionColumn;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

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
