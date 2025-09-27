package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.ExpressionColumn;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class ExpressionColumnRenderer implements Renderer<ExpressionColumn> {
    @Override
    public void render(ExpressionColumn entity, RenderContext ctx, SqlWriter w) {
        w.append(entity.expression());

        var alias = entity.alias();
        if (alias != null && !alias.isBlank()) {
            var quoter = ctx.dialect().quoter();
            w.space().append("AS").space().append(quoter.quoteIfNeeded(alias));
        }
    }
}
