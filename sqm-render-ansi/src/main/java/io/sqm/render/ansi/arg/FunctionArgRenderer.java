package io.sqm.render.ansi.arg;

import io.sqm.core.FunctionColumn;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

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
