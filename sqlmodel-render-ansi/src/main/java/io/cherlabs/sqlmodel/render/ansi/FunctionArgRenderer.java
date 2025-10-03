package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.FunctionColumn;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

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
