package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.NamedColumn;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class NamedColumnRenderer implements Renderer<NamedColumn> {
    @Override
    public Class<NamedColumn> targetType() {
        return NamedColumn.class;
    }

    @Override
    public void render(NamedColumn entity, RenderContext ctx, SqlWriter w) {
        var quoter = ctx.dialect().quoter();
        var table = entity.table();
        if (table != null && !table.isBlank()) {
            w.append(quoter.quoteIfNeeded(table));
            w.append(".");
        }
        w.append(quoter.quoteIfNeeded(entity.name()));

        var alias = entity.alias();
        if (alias != null && !alias.isBlank()) {
            w.space().append("AS").space().append(quoter.quoteIfNeeded(alias));
        }
    }
}
