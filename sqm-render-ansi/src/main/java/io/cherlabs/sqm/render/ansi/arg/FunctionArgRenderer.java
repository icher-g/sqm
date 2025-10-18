package io.cherlabs.sqm.render.ansi.arg;

import io.cherlabs.sqm.core.FunctionColumn;
import io.cherlabs.sqm.render.spi.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

public class FunctionArgRenderer implements Renderer<FunctionColumn.Arg.Function> {
    @Override
    public Class<FunctionColumn.Arg.Function> targetType() {
        return FunctionColumn.Arg.Function.class;
    }

    @Override
    public void render(FunctionColumn.Arg.Function entity, RenderContext ctx, SqlWriter w) {
        w.append(entity.call());
    }
}
