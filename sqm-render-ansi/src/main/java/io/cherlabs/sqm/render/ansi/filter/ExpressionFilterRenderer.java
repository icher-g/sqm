package io.cherlabs.sqm.render.ansi.filter;

import io.cherlabs.sqm.core.ExpressionFilter;
import io.cherlabs.sqm.render.spi.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

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
