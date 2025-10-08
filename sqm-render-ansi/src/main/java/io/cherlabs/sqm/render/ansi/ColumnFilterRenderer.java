package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.ColumnFilter;
import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

public class ColumnFilterRenderer implements Renderer<ColumnFilter> {
    @Override
    public Class<ColumnFilter> targetType() {
        return ColumnFilter.class;
    }

    @Override
    public void render(ColumnFilter entity, RenderContext ctx, SqlWriter w) {

        w.append(entity.column());
        w.space();

        switch (entity.op()) {
            case In -> renderIn(entity.values(), ctx, w);
            case NotIn -> renderNotIn(entity.values(), ctx, w);
            case Range -> renderRange(entity.values(), w);
            case Eq -> renderEq(entity.values(), ctx, w);
            case Ne -> renderNe(entity.values(), ctx, w);
            case Lt -> renderLt(entity.values(), ctx, w);
            case Lte -> renderLte(entity.values(), ctx, w);
            case Gt -> renderGt(entity.values(), ctx, w);
            case Gte -> renderGte(entity.values(), ctx, w);
            case Like -> renderLike(entity.values(), ctx, w);
            case NotLike -> renderNotLike(entity.values(), ctx, w);
            case IsNull -> renderIsNull(entity.values(), ctx, w);
            case IsNotNull -> renderIsNotNull(entity.values(), ctx, w);
            default -> throw new UnsupportedOperationException("The specified op: " + entity.op() + " is not supported.");
        }
    }

    private void renderIn(Values values, RenderContext ctx, SqlWriter w) {
        if (values instanceof Values.Single) {
            renderEq(values, ctx, w);
        } else if (values instanceof Values.ListValues l) {
            w.append(ctx.dialect().operators().in()).space().append(l);
        } else if (values instanceof Values.Subquery q) {
            w.append(ctx.dialect().operators().in()).space().append(q);
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'IN' op.");
        }
    }

    private void renderNotIn(Values values, RenderContext ctx, SqlWriter w) {
        if (values instanceof Values.Single) {
            renderNe(values, ctx, w);
        } else if (values instanceof Values.ListValues l) {
            w.append(ctx.dialect().operators().notIn()).space().append(l);
        } else if (values instanceof Values.Subquery q) {
            w.append(ctx.dialect().operators().notIn()).space().append(q);
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'NotIn' op.");
        }
    }

    private void renderRange(Values values, SqlWriter w) {
        if (values instanceof Values.Range r) {
            w.append(r);
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'Range' op.");
        }
    }

    private void renderEq(Values values, RenderContext ctx, SqlWriter w) {
        if (values instanceof Values.Single s) {
            w.append(ctx.dialect().operators().eq()).space().append(s);
        } else if (values instanceof Values.Column c) {
            w.append(ctx.dialect().operators().eq()).space().append(c);
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'Eq' op.");
        }
    }

    private void renderNe(Values values, RenderContext ctx, SqlWriter w) {
        if (values instanceof Values.Single s) {
            w.append(ctx.dialect().operators().ne()).space().append(s);
        } else if (values instanceof Values.Column c) {
            w.append(ctx.dialect().operators().ne()).space().append(c);
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'Ne' op.");
        }
    }

    private void renderLt(Values values, RenderContext ctx, SqlWriter w) {
        if (values instanceof Values.Single s) {
            w.append(ctx.dialect().operators().lt()).space().append(s);
        } else if (values instanceof Values.Column c) {
            w.append(ctx.dialect().operators().lt()).space().append(c);
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'Lt' op.");
        }
    }

    private void renderLte(Values values, RenderContext ctx, SqlWriter w) {
        if (values instanceof Values.Single s) {
            w.append(ctx.dialect().operators().lte()).space().append(s);
        } else if (values instanceof Values.Column c) {
            w.append(ctx.dialect().operators().lte()).space().append(c);
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'Lte' op.");
        }
    }

    private void renderGt(Values values, RenderContext ctx, SqlWriter w) {
        if (values instanceof Values.Single s) {
            w.append(ctx.dialect().operators().gt()).space().append(s);
        } else if (values instanceof Values.Column c) {
            w.append(ctx.dialect().operators().gt()).space().append(c);
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'Gt' op.");
        }
    }

    private void renderGte(Values values, RenderContext ctx, SqlWriter w) {
        if (values instanceof Values.Single s) {
            w.append(ctx.dialect().operators().gte()).space().append(s);
        } else if (values instanceof Values.Column c) {
            w.append(ctx.dialect().operators().gte()).space().append(c);
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'Gte' op.");
        }
    }

    private void renderLike(Values values, RenderContext ctx, SqlWriter w) {
        if (values instanceof Values.Single s) {
            w.append(ctx.dialect().operators().like()).space().append(s);
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'Like' op.");
        }
    }

    private void renderNotLike(Values values, RenderContext ctx, SqlWriter w) {
        if (values instanceof Values.Single s) {
            w.append(ctx.dialect().operators().notLike()).space().append(s);
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'NotLike' op.");
        }
    }

    private void renderIsNull(Values values, RenderContext ctx, SqlWriter w) {
        if (values == null || values instanceof Values.Single) {
            w.append(ctx.dialect().operators().isNull());
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'IsNull' op.");
        }
    }

    private void renderIsNotNull(Values values, RenderContext ctx, SqlWriter w) {
        if (values == null || values instanceof Values.Single) {
            w.append(ctx.dialect().operators().isNotNull());
        } else {
            throw new UnsupportedOperationException("The specified value type: " + values.getClass().getSimpleName() + " is not supported in 'IsNotNull' op.");
        }
    }
}
