package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.QueryTable;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

public class QueryTableRenderer implements Renderer<QueryTable> {
    @Override
    public Class<QueryTable> targetType() {
        return QueryTable.class;
    }

    @Override
    public void render(QueryTable entity, RenderContext ctx, SqlWriter w) {
        w.append("(").newline().indent();
        w.append(entity.query());
        w.outdent().append(")");

        var alias = entity.alias();
        if (alias != null && !alias.isBlank()) {
            var quoter = ctx.dialect().quoter();
            w.space().append("AS").space().append(quoter.quoteIfNeeded(alias));
        }
    }
}
