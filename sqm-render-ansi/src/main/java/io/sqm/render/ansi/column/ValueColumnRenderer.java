package io.sqm.render.ansi.column;

import io.sqm.core.ValueColumn;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

public class ValueColumnRenderer implements Renderer<ValueColumn> {
    @Override
    public Class<ValueColumn> targetType() {
        return ValueColumn.class;
    }

    @Override
    public void render(ValueColumn entity, RenderContext ctx, SqlWriter w) {
        w.append(ctx.dialect().formatter().format(entity.value()));

        var alias = entity.alias();
        if (alias != null && !alias.isBlank()) {
            var quoter = ctx.dialect().quoter();
            w.space().append("AS").space().append(quoter.quoteIfNeeded(alias));
        }
    }
}
