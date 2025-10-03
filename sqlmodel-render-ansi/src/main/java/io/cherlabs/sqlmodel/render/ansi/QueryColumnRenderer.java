package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.QueryColumn;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

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
