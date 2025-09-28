package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.CteQuery;
import io.cherlabs.sqlmodel.core.Query;
import io.cherlabs.sqlmodel.core.WithQuery;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class WithQueryRenderer implements Renderer<WithQuery> {
    @Override
    public void render(WithQuery entity, RenderContext ctx, SqlWriter w) {
        var queries = entity.getQueries();
        if (queries == null || queries.isEmpty()) {
            throw new IllegalArgumentException("WITH requires at least one query (CTE + outer query).");
        }

        w.append("WITH");
        if (entity.isRecursive()) w.space().append("RECURSIVE");
        w.newline();
        w.indent();

        for (int i = 0; i < queries.size(); i++) {
            if (i > 0) w.append(",").newline();
            var query = queries.get(i);
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
        renderer.render(entity, ctx, w);
    }
}
