package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.CteQuery;
import io.cherlabs.sqlmodel.core.Query;
import io.cherlabs.sqlmodel.core.WithQuery;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class WithQueryRenderer implements Renderer<WithQuery> {
    @Override
    public Class<WithQuery> targetType() {
        return WithQuery.class;
    }

    @Override
    public void render(WithQuery with, RenderContext ctx, SqlWriter w) {
        var ctes = with.ctes();
        if (ctes == null || ctes.isEmpty()) {
            throw new IllegalArgumentException("WITH requires at least one query (CTE + outer query).");
        }

        w.append("WITH");
        if (with.isRecursive()) w.space().append("RECURSIVE");
        w.newline();
        w.indent();

        for (int i = 0; i < ctes.size(); i++) {
            if (i > 0) w.append(",").newline();
            var query = ctes.get(i);
            if (query instanceof CteQuery cte) {
                w.append(cte);
            } else {
                w.append(query.name()).space().append("AS").space().append("(").newline();
                w.indent();
                w.append(query);
                w.outdent();
                w.append(")");
            }
        }
        w.newline();
        w.outdent();

        var renderer = ctx.dialect().renderers().require(Query.class);
        renderer.render(with, ctx, w);
    }
}
