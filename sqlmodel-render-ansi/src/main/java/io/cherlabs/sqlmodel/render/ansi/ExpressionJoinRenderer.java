package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.ExpressionJoin;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class ExpressionJoinRenderer implements Renderer<ExpressionJoin> {
    @Override
    public void render(ExpressionJoin entity, RenderContext ctx, SqlWriter w) {
        w.append(entity.expr());
    }
}
