package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

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
