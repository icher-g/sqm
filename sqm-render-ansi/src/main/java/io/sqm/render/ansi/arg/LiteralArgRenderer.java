package io.sqm.render.ansi.arg;

import io.sqm.core.FunctionColumn;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

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
