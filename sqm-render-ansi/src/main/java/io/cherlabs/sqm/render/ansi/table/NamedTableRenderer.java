package io.cherlabs.sqm.render.ansi.table;

import io.cherlabs.sqm.core.NamedTable;
import io.cherlabs.sqm.render.spi.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

public class NamedTableRenderer implements Renderer<NamedTable> {
    @Override
    public Class<NamedTable> targetType() {
        return NamedTable.class;
    }

    @Override
    public void render(NamedTable entity, RenderContext ctx, SqlWriter w) {
        var quoter = ctx.dialect().quoter();
        var schema = entity.schema();
        if (schema != null && !schema.isBlank()) {
            w.append(quoter.quoteIfNeeded(schema));
            w.append(".");
        }
        w.append(quoter.quoteIfNeeded(entity.name()));

        var alias = entity.alias();
        if (alias != null && !alias.isBlank()) {
            w.space().append("AS").space().append(quoter.quoteIfNeeded(alias));
        }
    }
}
