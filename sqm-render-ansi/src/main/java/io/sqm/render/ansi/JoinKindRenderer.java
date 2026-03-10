package io.sqm.render.ansi;

import io.sqm.core.JoinKind;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders join kinds as ANSI SQL join keywords.
 */
public class JoinKindRenderer {

    /**
     * Creates a join-kind renderer.
     */
    public JoinKindRenderer() {
    }

    /**
     * Renders a join kind.
     *
     * @param kind the join kind to render.
     * @param ctx  the active render context.
     * @param w    the SQL writer.
     */
    @SuppressWarnings("unused")
    public void render(JoinKind kind, RenderContext ctx, SqlWriter w) {
        switch (kind) {
            case INNER -> w.append("INNER JOIN");
            case LEFT -> w.append("LEFT JOIN");
            case RIGHT -> w.append("RIGHT JOIN");
            case FULL -> w.append("FULL JOIN");
            case STRAIGHT -> {
                if (!ctx.dialect().capabilities().supports(SqlFeature.STRAIGHT_JOIN)) {
                    throw new UnsupportedDialectFeatureException("STRAIGHT_JOIN", ctx.dialect().name());
                }
                w.append("STRAIGHT_JOIN");
            }
            default -> throw new IllegalStateException("Unexpected value: " + kind);
        }
    }
}
