package io.sqm.render.ansi;

import io.sqm.core.HierarchicalQueryClause;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders hierarchical query clauses, or rejects them when unsupported.
 */
public class HierarchicalQueryClauseRenderer implements Renderer<HierarchicalQueryClause> {
    /**
     * Creates a hierarchical-query renderer.
     */
    public HierarchicalQueryClauseRenderer() {
    }

    /**
     * Rejects hierarchical query rendering for unsupported dialects.
     *
     * @param node hierarchical query clause
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(HierarchicalQueryClause node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException(SqlFeature.HIERARCHICAL_QUERY.description(), ctx.dialect().name());
    }

    /**
     * Returns this renderer target type.
     *
     * @return {@link HierarchicalQueryClause} class
     */
    @Override
    public Class<HierarchicalQueryClause> targetType() {
        return HierarchicalQueryClause.class;
    }
}
