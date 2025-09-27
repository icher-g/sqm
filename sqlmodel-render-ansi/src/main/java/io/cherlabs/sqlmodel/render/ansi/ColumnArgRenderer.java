package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.FunctionColumn;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class ColumnArgRenderer implements Renderer<FunctionColumn.Arg.Column> {
    @Override
    public void render(FunctionColumn.Arg.Column entity, RenderContext ctx, SqlWriter w) {
        var quoter = ctx.dialect().quoter();
        var table = entity.table();
        if (table != null && !table.isBlank()) {
            w.append(quoter.quoteIfNeeded(table));
            w.append(".");
        }
        w.append(quoter.quoteIfNeeded(entity.name()));
    }
}
