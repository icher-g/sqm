package io.cherlabs.sqm.render.ansi.arg;

import io.cherlabs.sqm.core.FunctionColumn;
import io.cherlabs.sqm.render.spi.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

public class ColumnArgRenderer implements Renderer<FunctionColumn.Arg.Column> {
    @Override
    public Class<FunctionColumn.Arg.Column> targetType() {
        return FunctionColumn.Arg.Column.class;
    }

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
