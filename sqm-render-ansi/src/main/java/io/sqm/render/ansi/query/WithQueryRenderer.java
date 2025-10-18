package io.sqm.render.ansi.query;

import io.sqm.core.WithQuery;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

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
        if (with.recursive()) w.space().append("RECURSIVE");
        w.newline();
        w.indent();

        for (int i = 0; i < ctes.size(); i++) {
            if (i > 0) w.append(",").newline();
            w.append(ctes.get(i));
        }
        w.newline();
        w.outdent();
        w.append(with.body());
    }
}
