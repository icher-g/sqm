package io.sqm.render.ansi;

import io.sqm.core.TimeZoneSpec;
import io.sqm.core.TimestampLiteralExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders ANSI {@code TIMESTAMP '...'} literals.
 */
public class TimestampLiteralExprRenderer implements Renderer<TimestampLiteralExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(TimestampLiteralExpr node, RenderContext ctx, SqlWriter w) {
        w.append("TIMESTAMP");
        if (node.timeZoneSpec() == TimeZoneSpec.WITH_TIME_ZONE) {
            w.append(" WITH TIME ZONE");
        }
        else if (node.timeZoneSpec() == TimeZoneSpec.WITHOUT_TIME_ZONE) {
            w.append(" WITHOUT TIME ZONE");
        }
        w.append(" ").append(ctx.dialect().formatter().format(node.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<TimestampLiteralExpr> targetType() {
        return TimestampLiteralExpr.class;
    }
}
