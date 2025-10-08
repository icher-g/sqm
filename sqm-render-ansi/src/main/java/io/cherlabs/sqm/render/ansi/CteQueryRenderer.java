package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.CteQuery;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

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

        var renderer = ctx.dialect().renderers().require(Query.class);
        renderer.render(entity, ctx, w);

        w.outdent();
        w.append(")");
    }
}
