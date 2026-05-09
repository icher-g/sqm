package io.sqm.render.oracle;

import io.sqm.core.HierarchicalQueryClause;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle hierarchical query clauses.
 */
public class HierarchicalQueryClauseRenderer extends io.sqm.render.ansi.HierarchicalQueryClauseRenderer {
    /**
     * Creates an Oracle hierarchical-query renderer.
     */
    public HierarchicalQueryClauseRenderer() {
    }

    /**
     * Renders an Oracle hierarchical query clause.
     *
     * @param node hierarchical query clause
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(HierarchicalQueryClause node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.HIERARCHICAL_QUERY)) {
            throw new UnsupportedDialectFeatureException(SqlFeature.HIERARCHICAL_QUERY.description(), ctx.dialect().name());
        }
        if (node.startWith() != null) {
            w.append("START WITH").space().append(node.startWith()).newline();
        }
        w.append("CONNECT BY").space();
        if (node.noCycle()) {
            w.append("NOCYCLE").space();
        }
        w.append(node.connectBy());
        if (node.orderSiblingsBy() != null) {
            w.newline().append("ORDER SIBLINGS BY").space();
            w.comma(node.orderSiblingsBy().items());
        }
    }
}
