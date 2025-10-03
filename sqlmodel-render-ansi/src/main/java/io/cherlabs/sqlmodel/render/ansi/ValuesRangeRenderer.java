package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Values;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class ValuesRangeRenderer implements Renderer<Values.Range> {
    @Override
    public Class<Values.Range> targetType() {
        return Values.Range.class;
    }

    @Override
    public void render(Values.Range entity, RenderContext ctx, SqlWriter w) {
        var min = ctx.bindOrFormat(entity.min());
        var max = ctx.bindOrFormat(entity.max());
        var ops = ctx.dialect().operators();
        w.append(ops.range()).space().append(min).space().append(ops.and()).space().append(max);
    }
}
