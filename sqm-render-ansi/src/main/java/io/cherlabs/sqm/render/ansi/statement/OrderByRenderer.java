package io.cherlabs.sqm.render.ansi.statement;

import io.cherlabs.sqm.core.OrderBy;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;
import io.cherlabs.sqm.render.spi.Renderer;

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
