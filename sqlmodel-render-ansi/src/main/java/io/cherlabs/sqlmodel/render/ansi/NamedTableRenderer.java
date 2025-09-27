package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.NamedTable;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class NamedTableRenderer implements Renderer<NamedTable> {
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
