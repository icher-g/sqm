package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.TableJoin;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class TableJoinRenderer implements Renderer<TableJoin> {
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
