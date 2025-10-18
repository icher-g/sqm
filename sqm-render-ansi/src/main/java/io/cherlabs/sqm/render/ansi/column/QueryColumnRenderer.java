package io.cherlabs.sqm.render.ansi.column;

import io.cherlabs.sqm.core.QueryColumn;
import io.cherlabs.sqm.render.spi.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

public class QueryColumnRenderer implements Renderer<QueryColumn> {
    @Override
    public Class<QueryColumn> targetType() {
        return QueryColumn.class;
    }

    @Override
    public void render(QueryColumn entity, RenderContext ctx, SqlWriter w) {

        w.ignoreNewLine(true);
        w.append("(").append(entity.query()).append(")");
        w.ignoreNewLine(false);

        var alias = entity.alias();
        if (alias != null && !alias.isBlank()) {
            var quoter = ctx.dialect().quoter();
            w.space().append("AS").space().append(quoter.quoteIfNeeded(alias));
        }
    }
}
