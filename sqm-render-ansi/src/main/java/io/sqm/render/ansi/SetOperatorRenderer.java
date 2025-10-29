package io.sqm.render.ansi;

import io.sqm.core.SetOperator;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

public class SetOperatorRenderer {

    public void render(SetOperator op, RenderContext ctx, SqlWriter w) {
        switch (op) {
            case UNION -> w.append("UNION");
            case UNION_ALL -> w.append("UNION ALL");
            case INTERSECT -> w.append("INTERSECT");
            case INTERSECT_ALL -> w.append("INTERSECT ALL");
            case EXCEPT -> w.append("EXCEPT");
            case EXCEPT_ALL -> w.append("EXCEPT ALL");
            default -> throw new IllegalStateException("Unexpected value: " + op);
        }
    }
}
