package io.sqm.render.ansi.query;

import io.sqm.core.CteQuery;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

public class CteQueryRenderer implements Renderer<CteQuery> {
    @Override
    public Class<CteQuery> targetType() {
        return CteQuery.class;
    }

    @Override
    public void render(CteQuery entity, RenderContext ctx, SqlWriter w) {
        var name = entity.name();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("CteQuery must have name.");
        }

        var quoter = ctx.dialect().quoter();
        w.append(entity.name());

        var aliases = entity.columnAliases();
        if (aliases != null && !aliases.isEmpty()) {
            w.space().append("(");
            for (int i = 0; i < aliases.size(); i++) {
                if (i > 0) w.append(", ");
                w.append(quoter.quoteIfNeeded(aliases.get(i)));
            }
            w.append(")");
        }

        w.space().append("AS").space().append("(").newline();
        w.indent();
        w.append(entity.body());
        w.outdent();
        w.append(")");
    }
}
