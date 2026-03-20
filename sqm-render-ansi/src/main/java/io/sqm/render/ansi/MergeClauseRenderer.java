package io.sqm.render.ansi;

import io.sqm.core.MergeClause;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Baseline ANSI renderer for {@link MergeClause}.
 */
public class MergeClauseRenderer implements Renderer<MergeClause> {

    /**
     * Creates a merge-clause renderer.
     */
    public MergeClauseRenderer() {
    }

    @Override
    public void render(MergeClause node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MERGE clause", ctx.dialect().name());
    }

    /**
     * Renders the shared MERGE-clause subset used by dialect-specific implementations.
     *
     * @param node merge clause to render
     * @param w SQL writer
     */
    protected final void renderSupportedClause(MergeClause node, SqlWriter w) {
        w.append("WHEN");
        renderMatchType(node, w);
        if (node.condition() != null) {
            w.space().append("AND").space().append(node.condition());
        }
        w.space().append("THEN").space().append(node.action());
    }

    /**
     * Renders the branch label for a MERGE clause.
     *
     * @param node merge clause to render
     * @param w SQL writer
     */
    protected void renderMatchType(MergeClause node, SqlWriter w) {
        switch (node.matchType()) {
            case MATCHED -> w.space().append("MATCHED");
            case NOT_MATCHED -> w.space().append("NOT").space().append("MATCHED");
            case NOT_MATCHED_BY_SOURCE -> w.space().append("NOT").space().append("MATCHED").space().append("BY").space().append("SOURCE");
        }
    }

    @Override
    public Class<MergeClause> targetType() {
        return MergeClause.class;
    }
}
