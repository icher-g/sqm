package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.TupleFilter;
import io.cherlabs.sqlmodel.core.Values;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class TupleFilterRenderer implements Renderer<TupleFilter> {
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
