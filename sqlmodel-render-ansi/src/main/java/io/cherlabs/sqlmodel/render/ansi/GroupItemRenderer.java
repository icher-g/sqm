package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.GroupItem;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class GroupItemRenderer implements Renderer<GroupItem> {
    @Override
    public void render(GroupItem entity, RenderContext ctx, SqlWriter w) {
        if (entity.isOrdinal()) {
            w.append(Integer.toString(entity.ordinal()));
        } else {
            w.append(entity.column());
        }
    }
}
