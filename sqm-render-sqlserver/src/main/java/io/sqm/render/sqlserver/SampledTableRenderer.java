package io.sqm.render.sqlserver;

import io.sqm.core.SampledTable;
import io.sqm.core.TableSampleSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server sampled table references.
 */
public class SampledTableRenderer extends io.sqm.render.ansi.SampledTableRenderer {
    /**
     * Creates a SQL Server sampled table renderer.
     */
    public SampledTableRenderer() {
    }

    @Override
    public void render(SampledTable node, RenderContext ctx, SqlWriter w) {
        w.append(node.source()).space().append("TABLESAMPLE").space().append("(");
        w.append(node.sampleSpec().amount());
        if (node.sampleSpec().unit() == TableSampleSpec.SampleUnit.PERCENT) {
            w.space().append("PERCENT");
        }
        else if (node.sampleSpec().unit() == TableSampleSpec.SampleUnit.ROWS) {
            w.space().append("ROWS");
        }
        w.append(")");
        if (node.sampleSpec().repeatableSeed() != null) {
            w.space().append("REPEATABLE").space().append("(").append(node.sampleSpec().repeatableSeed()).append(")");
        }
        if (node.alias() != null) {
            w.space().append("AS").space().append(renderIdentifier(node.alias(), ctx.dialect().quoter()));
        }
    }
}
