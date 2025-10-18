package io.sqm.render.ansi.column;

import io.sqm.core.ExpressionColumn;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

public class ExpressionColumnRenderer implements Renderer<ExpressionColumn> {
    @Override
    public Class<ExpressionColumn> targetType() {
        return ExpressionColumn.class;
    }

    @Override
    public void render(ExpressionColumn entity, RenderContext ctx, SqlWriter w) {
        w.append(entity.expr());

        var alias = entity.alias();
        if (alias != null && !alias.isBlank()) {
            var quoter = ctx.dialect().quoter();
            w.space().append("AS").space().append(quoter.quoteIfNeeded(alias));
        }
    }
}
