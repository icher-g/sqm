package io.sqm.render.ansi.join;

import io.sqm.core.ExpressionJoin;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

public class ExpressionJoinRenderer implements Renderer<ExpressionJoin> {
    @Override
    public Class<ExpressionJoin> targetType() {
        return ExpressionJoin.class;
    }

    @Override
    public void render(ExpressionJoin entity, RenderContext ctx, SqlWriter w) {
        w.append(entity.expr());
    }
}
