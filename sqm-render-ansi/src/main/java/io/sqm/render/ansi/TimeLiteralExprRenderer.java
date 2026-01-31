package io.sqm.render.ansi;

import io.sqm.core.TimeLiteralExpr;
import io.sqm.core.TimeZoneSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders ANSI {@code TIME '...'} literals.
 */
public class TimeLiteralExprRenderer implements Renderer<TimeLiteralExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(TimeLiteralExpr node, RenderContext ctx, SqlWriter w) {
        w.append("TIME");
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
    public Class<TimeLiteralExpr> targetType() {
        return TimeLiteralExpr.class;
    }
}
