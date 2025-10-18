package io.sqm.render.ansi.join;

import io.sqm.core.TableJoin;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

public class TableJoinRenderer implements Renderer<TableJoin> {
    @Override
    public Class<TableJoin> targetType() {
        return TableJoin.class;
    }

    @Override
    public void render(TableJoin entity, RenderContext ctx, SqlWriter w) {
        switch (entity.joinType()) {
            case Inner -> {
                w.append("INNER JOIN").space();
                w.append(entity.table()).space().append("ON").space().append(entity.on());
            }
            case Left -> {
                w.append("LEFT JOIN").space();
                w.append(entity.table()).space().append("ON").space().append(entity.on());
            }
            case Right -> {
                w.append("RIGHT JOIN").space();
                w.append(entity.table()).space().append("ON").space().append(entity.on());
            }
            case Full -> {
                w.append("FULL JOIN").space();
                w.append(entity.table()).space().append("ON").space().append(entity.on());
            }
            case Cross -> {
                w.append("CROSS JOIN").space();
                w.append(entity.table());
            }
        }
    }
}
