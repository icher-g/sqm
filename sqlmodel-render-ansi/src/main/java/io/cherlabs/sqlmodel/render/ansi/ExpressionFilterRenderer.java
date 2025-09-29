package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.ExpressionFilter;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class ExpressionFilterRenderer implements Renderer<ExpressionFilter> {
    @Override
    public void render(ExpressionFilter entity, RenderContext ctx, SqlWriter w) {
        w.append(entity.expr());
    }
}
