package io.sqm.render.ansi.statement;

import io.sqm.core.OrderBy;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class OrderByRenderer implements Renderer<OrderBy> {

    @Override
    public void render(OrderBy entity, RenderContext ctx, SqlWriter w) {
        w.comma(entity.items()); // order items with direction/nulls etc. via item renderer
    }

    @Override
    public Class<OrderBy> targetType() {
        return OrderBy.class;
    }
}
