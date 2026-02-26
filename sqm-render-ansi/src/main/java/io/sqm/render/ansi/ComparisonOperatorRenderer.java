package io.sqm.render.ansi;

import io.sqm.core.ComparisonOperator;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders comparison operators using dialect-specific tokens.
 */
public class ComparisonOperatorRenderer {

    /**
     * Creates a comparison-operator renderer.
     */
    public ComparisonOperatorRenderer() {
    }

    /**
     * Renders a comparison operator.
     *
     * @param op  the operator to render.
     * @param ctx the active render context.
     * @param w   the SQL writer.
     */
    public void render(ComparisonOperator op, RenderContext ctx, SqlWriter w) {
        switch (op) {
            case EQ -> w.append(ctx.dialect().operators().eq());
            case NE -> w.append(ctx.dialect().operators().ne());
            case LT -> w.append(ctx.dialect().operators().lt());
            case LTE -> w.append(ctx.dialect().operators().lte());
            case GT -> w.append(ctx.dialect().operators().gt());
            case GTE -> w.append(ctx.dialect().operators().gte());
            default -> throw new IllegalStateException("Unexpected value: " + op);
        }
    }
}
