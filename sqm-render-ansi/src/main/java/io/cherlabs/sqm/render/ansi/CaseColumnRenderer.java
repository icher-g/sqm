package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.CaseColumn;
import io.cherlabs.sqm.core.WhenThen;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

public class CaseColumnRenderer implements Renderer<CaseColumn> {
    @Override
    public Class<CaseColumn> targetType() {
        return CaseColumn.class;
    }

    @Override
    public void render(CaseColumn entity, RenderContext ctx, SqlWriter w) {
        w.append("CASE");

        for (WhenThen arm : entity.whens()) {
            w.space().append("WHEN").space();
            w.append(arm.when());
            w.space().append("THEN").space();
            w.append(arm.then());
        }

        if (entity.elseValue() != null) {
            w.space().append("ELSE").space();
            w.append(entity.elseValue());
        }

        w.space().append("END");

        var alias = entity.alias();
        if (alias != null && !alias.isBlank()) {
            var quoter = ctx.dialect().quoter();
            w.space().append("AS").space().append(quoter.quoteIfNeeded(alias));
        }
    }
}
