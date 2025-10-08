package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

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
