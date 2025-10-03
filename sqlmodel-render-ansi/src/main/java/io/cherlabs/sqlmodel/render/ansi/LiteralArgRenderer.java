package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.FunctionColumn;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class LiteralArgRenderer implements Renderer<FunctionColumn.Arg.Literal> {
    @Override
    public Class<FunctionColumn.Arg.Literal> targetType() {
        return FunctionColumn.Arg.Literal.class;
    }

    @Override
    public void render(FunctionColumn.Arg.Literal entity, RenderContext ctx, SqlWriter w) {
        w.append(ctx.dialect().formatter().format(entity.value()));
    }
}
