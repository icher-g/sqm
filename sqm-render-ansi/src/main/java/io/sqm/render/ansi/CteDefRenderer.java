package io.sqm.render.ansi;

import io.sqm.core.CteDef;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class CteDefRenderer implements Renderer<CteDef> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(CteDef node, RenderContext ctx, SqlWriter w) {
        var name = node.name();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("CteQuery must have name.");
        }

        var quoter = ctx.dialect().quoter();
        w.append(node.name());

        var aliases = node.columnAliases();
        if (aliases != null && !aliases.isEmpty()) {
            w.space().append("(");
            for (int i = 0; i < aliases.size(); i++) {
                if (i > 0) w.append(", ");
                w.append(quoter.quoteIfNeeded(aliases.get(i)));
            }
            w.append(")");
        }

        w.space().append("AS");

        if (node.materialization() == CteDef.Materialization.MATERIALIZED) {
            if (!ctx.dialect().capabilities().supports(SqlFeature.CTE_MATERIALIZATION)) {
                throw new UnsupportedDialectFeatureException("CTE MATERIALIZED", ctx.dialect().name());
            }
            w.space().append("MATERIALIZED");
        }
        else if (node.materialization() == CteDef.Materialization.NOT_MATERIALIZED) {
            if (!ctx.dialect().capabilities().supports(SqlFeature.CTE_MATERIALIZATION)) {
                throw new UnsupportedDialectFeatureException("CTE NOT MATERIALIZED", ctx.dialect().name());
            }
            w.space().append("NOT MATERIALIZED");
        }

        w.space();
        w.append(node.body(), true, true);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<CteDef> targetType() {
        return CteDef.class;
    }
}
