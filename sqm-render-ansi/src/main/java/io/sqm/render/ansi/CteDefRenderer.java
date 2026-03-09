package io.sqm.render.ansi;

import io.sqm.core.CteDef;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders common table expression (CTE) definitions.
 */
public class CteDefRenderer implements Renderer<CteDef> {
    /**
     * Creates a CTE-definition renderer.
     */
    public CteDefRenderer() {
    }

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
        if (name == null) {
            throw new IllegalArgumentException("CteQuery must have name.");
        }

        var quoter = ctx.dialect().quoter();
        w.append(renderIdentifier(name, quoter));

        var aliases = node.columnAliases();
        if (aliases != null && !aliases.isEmpty()) {
            w.space().append("(");
            for (int i = 0; i < aliases.size(); i++) {
                if (i > 0) w.append(", ");
                w.append(renderIdentifier(aliases.get(i), quoter));
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
        renderBody(node.body(), ctx, w);
    }

    /**
     * Renders CTE body.
     *
     * @param body CTE body statement.
     * @param ctx render context.
     * @param w SQL writer.
     */
    protected void renderBody(Statement body, RenderContext ctx, SqlWriter w) {
        if (body instanceof Query query) {
            w.append(query, true, true);
            return;
        }
        throw new UnsupportedDialectFeatureException("Writable CTE DML", ctx.dialect().name());
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
