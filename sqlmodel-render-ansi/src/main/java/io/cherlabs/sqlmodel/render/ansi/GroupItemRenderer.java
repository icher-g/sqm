package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Group;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class GroupItemRenderer implements Renderer<Group> {
    @Override
    public void render(Group entity, RenderContext ctx, SqlWriter w) {
        if (entity.isOrdinal()) {
            w.append(Integer.toString(entity.ordinal()));
        } else {
            w.append(entity.column());
        }
    }
}
