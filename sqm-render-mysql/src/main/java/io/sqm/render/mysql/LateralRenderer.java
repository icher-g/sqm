package io.sqm.render.mysql;

import io.sqm.core.Lateral;
import io.sqm.core.QueryTable;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders MySQL {@code LATERAL} derived tables.
 *
 * <p>MySQL supports {@code LATERAL} only for derived tables and requires an
 * alias on the derived table.</p>
 */
public final class LateralRenderer implements Renderer<Lateral> {
    /**
     * Creates a MySQL lateral renderer.
     */
    public LateralRenderer() {
    }

    /**
     * Renders a MySQL lateral derived table.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(Lateral node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.LATERAL)) {
            throw new UnsupportedDialectFeatureException("LATERAL", ctx.dialect().name());
        }
        if (!(node.inner() instanceof QueryTable queryTable)) {
            throw new IllegalArgumentException("MySQL LATERAL supports only derived tables");
        }
        if (queryTable.alias() == null) {
            throw new IllegalArgumentException("MySQL LATERAL derived tables require an alias");
        }
        w.append("LATERAL").space().append(queryTable);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends Lateral> targetType() {
        return Lateral.class;
    }
}