package io.sqm.render.ansi.value;

import io.sqm.core.Values;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

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
