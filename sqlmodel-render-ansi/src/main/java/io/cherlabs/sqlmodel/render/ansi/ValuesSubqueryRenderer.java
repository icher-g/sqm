package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Values;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

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
