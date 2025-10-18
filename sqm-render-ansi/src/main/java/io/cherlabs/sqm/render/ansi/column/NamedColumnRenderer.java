package io.cherlabs.sqm.render.ansi.column;

import io.cherlabs.sqm.core.NamedColumn;
import io.cherlabs.sqm.render.spi.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

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
