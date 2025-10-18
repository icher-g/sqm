package io.sqm.render.ansi.filter;

import io.sqm.core.ExpressionFilter;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

public class ExpressionFilterRenderer implements Renderer<ExpressionFilter> {
    @Override
    public Class<ExpressionFilter> targetType() {
        return ExpressionFilter.class;
    }

    @Override
    public void render(ExpressionFilter entity, RenderContext ctx, SqlWriter w) {
        w.append(entity.expr());
    }
}
