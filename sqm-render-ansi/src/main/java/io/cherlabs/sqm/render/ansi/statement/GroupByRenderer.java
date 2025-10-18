package io.cherlabs.sqm.render.ansi.statement;

import io.cherlabs.sqm.core.GroupBy;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;
import io.cherlabs.sqm.render.spi.Renderer;

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
