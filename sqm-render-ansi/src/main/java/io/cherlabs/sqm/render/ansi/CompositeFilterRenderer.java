package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.CompositeFilter;
import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlFragment;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

import static io.cherlabs.sqm.core.CompositeFilter.Operator.Not;

public class CompositeFilterRenderer implements Renderer<CompositeFilter> {
    @Override
    public Class<CompositeFilter> targetType() {
        return CompositeFilter.class;
    }

    @Override
    public void render(CompositeFilter entity, RenderContext ctx, SqlWriter w) {
        var operator = switch (entity.op()) {
            case And -> ctx.dialect().operators().and();
            case Or -> ctx.dialect().operators().or();
            case Not -> ctx.dialect().operators().not();
        };

        if (entity.filters().size() > 1) {
            for (int i = 0; i < entity.filters().size(); i++) {
                var filter = entity.filters().get(i);
                if (i > 0) {
                    w.newline().append(operator).space();
                }
                render(filter, ctx, w);
            }
        } else {
            var filter = entity.filters().get(0);
            if (entity.op() == Not) {
                w.append(operator).space().append("(");
            }
            render(filter, ctx, w);
            if (entity.op() == Not) {
                w.append(")");
            }
        }
    }

    private void render(Filter filter, RenderContext ctx, SqlWriter w) {
        var sql = SqlFragment.capture(ctx, writer -> writer.append(filter));
        if (filter instanceof CompositeFilter cf && cf.op() != Not) {
            w.indent().append("(").newline();
        }
        w.append(sql);
        if (filter instanceof CompositeFilter cf && cf.op() != Not) {
            w.outdent().newline().append(")");
        }
    }
}
