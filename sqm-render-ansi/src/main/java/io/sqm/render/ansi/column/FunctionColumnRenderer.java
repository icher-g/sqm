package io.sqm.render.ansi.column;

import io.sqm.core.FunctionColumn;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

public class FunctionColumnRenderer implements Renderer<FunctionColumn> {
    @Override
    public Class<FunctionColumn> targetType() {
        return FunctionColumn.class;
    }

    @Override
    public void render(FunctionColumn entity, RenderContext ctx, SqlWriter w) {
        // Render qualified function name with identifier quoting per dialect
        var quoter = ctx.dialect().quoter();
        var parts = entity.name().split("\\.");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) w.append(".");
            w.append(quoter.quoteIfNeeded(parts[i]));
        }

        w.append("(");

        if (entity.distinct()) {
            w.append("DISTINCT");
            // add space if there will be args after DISTINCT
            if (!entity.args().isEmpty()) w.space();
        }

        // Render arguments list
        w.comma(entity.args());
        w.append(")");

        // Optional alias
        var alias = entity.alias();
        if (alias != null && !alias.isBlank()) {
            w.space().append("AS").space().append(quoter.quoteIfNeeded(alias));
        }
    }
}
