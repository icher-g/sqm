package io.sqm.render.ansi;

import io.sqm.core.PivotTable;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders relational {@code PIVOT} table transforms.
 */
public class PivotTableRenderer implements Renderer<PivotTable> {
    /**
     * Creates a pivot table-reference renderer.
     */
    public PivotTableRenderer() {
    }

    /**
     * Renders the node into a SQL writer.
     *
     * @param node pivot table reference
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(PivotTable node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.PIVOT_TABLE)) {
            throw new UnsupportedDialectFeatureException("PIVOT", ctx.dialect().name());
        }

        w.append(node.source())
            .space()
            .append("PIVOT")
            .space()
            .append("(")
            .comma(node.measures())
            .space()
            .append("FOR")
            .space()
            .append(node.forExpression())
            .space()
            .append("IN")
            .space()
            .append("(")
            .comma(node.values())
            .append(")")
            .append(")");
        if (node.alias() != null) {
            w.space().append("AS").space().append(renderIdentifier(node.alias(), ctx.dialect().quoter()));
        }
    }

    /**
     * Gets the renderer target type.
     *
     * @return pivot table-reference type
     */
    @Override
    public Class<PivotTable> targetType() {
        return PivotTable.class;
    }
}
