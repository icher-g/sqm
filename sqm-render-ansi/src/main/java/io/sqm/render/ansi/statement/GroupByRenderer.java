package io.sqm.render.ansi.statement;

import io.sqm.core.GroupBy;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class GroupByRenderer implements Renderer<GroupBy> {

    @Override
    public void render(GroupBy entity, RenderContext ctx, SqlWriter w) {
        w.comma(entity.items()); // columns/expressions
    }

    @Override
    public Class<GroupBy> targetType() {
        return GroupBy.class;
    }
}
