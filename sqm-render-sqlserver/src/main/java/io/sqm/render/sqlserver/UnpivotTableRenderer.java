package io.sqm.render.sqlserver;

import io.sqm.core.UnpivotTable;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server {@code UNPIVOT} table transforms.
 */
public class UnpivotTableRenderer extends io.sqm.render.ansi.UnpivotTableRenderer {
    /**
     * Creates a SQL Server unpivot table-reference renderer.
     */
    public UnpivotTableRenderer() {
    }

    /**
     * Renders a SQL Server unpivot table transform.
     *
     * @param node unpivot table reference
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(UnpivotTable node, RenderContext ctx, SqlWriter w) {
        if (node.nullTreatment() != UnpivotTable.NullTreatment.DIALECT_DEFAULT) {
            throw new UnsupportedDialectFeatureException("UNPIVOT null treatment", ctx.dialect().name());
        }
        super.render(node, ctx, w);
    }
}
