package io.sqm.render.ansi.value;

import io.sqm.core.Values;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

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
