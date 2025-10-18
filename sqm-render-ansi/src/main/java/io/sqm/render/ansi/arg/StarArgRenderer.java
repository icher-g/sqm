package io.sqm.render.ansi.arg;

import io.sqm.core.FunctionColumn;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

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
