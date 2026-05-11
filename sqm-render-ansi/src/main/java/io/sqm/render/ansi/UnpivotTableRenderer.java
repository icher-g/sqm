package io.sqm.render.ansi;

import io.sqm.core.UnpivotTable;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders relational {@code UNPIVOT} table transforms.
 */
public class UnpivotTableRenderer implements Renderer<UnpivotTable> {
    /**
     * Creates an unpivot table-reference renderer.
     */
    public UnpivotTableRenderer() {
    }

    /**
     * Renders the node into a SQL writer.
     *
     * @param node unpivot table reference
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(UnpivotTable node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.UNPIVOT_TABLE)) {
            throw new UnsupportedDialectFeatureException("UNPIVOT", ctx.dialect().name());
        }

        var quoter = ctx.dialect().quoter();
        w.append(node.source())
            .space()
            .append("UNPIVOT");
        if (node.nullTreatment() == UnpivotTable.NullTreatment.INCLUDE_NULLS) {
            w.space().append("INCLUDE NULLS");
        }
        else if (node.nullTreatment() == UnpivotTable.NullTreatment.EXCLUDE_NULLS) {
            w.space().append("EXCLUDE NULLS");
        }
        w.space().append("(");
        if (node.valueColumns().size() == 1) {
            w.append(renderIdentifier(node.valueColumns().getFirst(), quoter));
        }
        else {
            w.append("(").comma(node.valueColumns(), quoter).append(")");
        }
        w.space()
            .append("FOR")
            .space()
            .append(renderIdentifier(node.nameColumn(), quoter))
            .space()
            .append("IN")
            .space()
            .append("(")
            .comma(node.inputs())
            .append(")")
            .append(")");
        if (node.alias() != null) {
            w.space().append("AS").space().append(renderIdentifier(node.alias(), quoter));
        }
    }

    /**
     * Gets the renderer target type.
     *
     * @return unpivot table-reference type
     */
    @Override
    public Class<UnpivotTable> targetType() {
        return UnpivotTable.class;
    }
}
