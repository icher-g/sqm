package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

public class ValuesSubqueryRenderer implements Renderer<Values.Subquery> {
    @Override
    public Class<Values.Subquery> targetType() {
        return Values.Subquery.class;
    }

    @Override
    public void render(Values.Subquery entity, RenderContext ctx, SqlWriter w) {
        w.ignoreNewLine(true);
        w.append("(").append(entity.query()).append(")");
        w.ignoreNewLine(false);
    }
}
