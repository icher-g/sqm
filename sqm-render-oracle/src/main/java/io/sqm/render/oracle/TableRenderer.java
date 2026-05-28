package io.sqm.render.oracle;

import io.sqm.core.Table;
import io.sqm.core.TablePartitionSpec;
import io.sqm.core.TableVersionSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle table access modifiers.
 */
public class TableRenderer extends io.sqm.render.ansi.TableRenderer {
    /**
     * Creates an Oracle table renderer.
     */
    public TableRenderer() {
    }

    @Override
    protected void renderTableVersion(Table node, RenderContext ctx, SqlWriter w) {
        TableVersionSpec version = node.version();
        if (version == null) {
            return;
        }
        switch (version.kind()) {
            case AS_OF_TIMESTAMP -> w.space().append("AS OF TIMESTAMP").space().append(version.value());
            case AS_OF_SCN -> w.space().append("AS OF SCN").space().append(version.value());
            default -> super.renderTableVersion(node, ctx, w);
        }
    }

    @Override
    protected void renderPartitionSpec(Table node, RenderContext ctx, SqlWriter w) {
        TablePartitionSpec selector = node.partitionSpec();
        if (selector == null) {
            return;
        }
        w.space().append(selector.kind() == TablePartitionSpec.TablePartitionSpecKind.SUBPARTITION ? "SUBPARTITION" : "PARTITION");
        w.space().append("(");
        w.comma(selector.names(), ctx.dialect().quoter());
        w.append(")");
    }
}
