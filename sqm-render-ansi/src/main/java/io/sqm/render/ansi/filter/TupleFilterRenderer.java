package io.sqm.render.ansi.filter;

import io.sqm.core.TupleFilter;
import io.sqm.core.Values;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

public class TupleFilterRenderer implements Renderer<TupleFilter> {
    @Override
    public Class<TupleFilter> targetType() {
        return TupleFilter.class;
    }

    @Override
    public void render(TupleFilter entity, RenderContext ctx, SqlWriter w) {

        w.append("(").comma(entity.columns()).append(")").space();

        switch (entity.operator()) {
            case In -> renderIn(entity.values(), ctx, w);
            case NotIn -> renderNotIn(entity.values(), ctx, w);
            default -> throw new UnsupportedOperationException("The specified op: " + entity.operator() + " is not supported.");
        }
    }

    private void renderIn(Values values, RenderContext ctx, SqlWriter w) {
        if (values instanceof Values.Tuples t) {
            w.append(ctx.dialect().operators().in()).space().append(ctx.dialect().formatter().format(t.rows()));
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'IN' op.");
        }
    }

    private void renderNotIn(Values values, RenderContext ctx, SqlWriter w) {
        if (values instanceof Values.Tuples t) {
            w.append(ctx.dialect().operators().notIn()).space().append(ctx.dialect().formatter().format(t.rows()));
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'NOT IN' op.");
        }
    }
}
