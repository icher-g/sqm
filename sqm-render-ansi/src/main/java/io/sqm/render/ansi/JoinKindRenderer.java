package io.sqm.render.ansi;

import io.sqm.core.JoinKind;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

public class JoinKindRenderer {

    public void render(JoinKind kind, RenderContext ctx, SqlWriter w) {
        switch (kind) {
            case INNER -> w.append("INNER JOIN");
            case LEFT -> w.append("LEFT JOIN");
            case RIGHT -> w.append("RIGHT JOIN");
            case FULL -> w.append("FULL JOIN");
            default -> throw new IllegalStateException("Unexpected value: " + kind);
        }
    }
}
