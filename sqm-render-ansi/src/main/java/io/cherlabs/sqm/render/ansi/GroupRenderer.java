package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Group;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

public class GroupRenderer implements Renderer<Group> {
    @Override
    public Class<Group> targetType() {
        return Group.class;
    }

    @Override
    public void render(Group entity, RenderContext ctx, SqlWriter w) {
        if (entity.isOrdinal()) {
            w.append(Integer.toString(entity.ordinal()));
        } else {
            w.append(entity.column());
        }
    }
}
