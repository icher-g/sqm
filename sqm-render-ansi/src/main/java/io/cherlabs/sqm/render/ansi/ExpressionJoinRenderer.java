package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.ExpressionJoin;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

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
