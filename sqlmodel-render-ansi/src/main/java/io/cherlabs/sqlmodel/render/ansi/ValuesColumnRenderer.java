package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Values;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class ValuesColumnRenderer implements Renderer<Values.Column> {
    @Override
    public Class<Values.Column> targetType() {
        return Values.Column.class;
    }

    @Override
    public void render(Values.Column entity, RenderContext ctx, SqlWriter w) {
        w.append(entity.column());
    }
}
