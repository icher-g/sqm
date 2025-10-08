package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.FunctionColumn;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

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
