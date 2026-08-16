package io.sqm.render.oracle;

import io.sqm.core.SampledTable;
import io.sqm.core.TableSampleSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle sampled table references.
 */
public class SampledTableRenderer extends io.sqm.render.ansi.SampledTableRenderer {
    /**
     * Creates an Oracle sampled table renderer.
     */
    public SampledTableRenderer() {
    }

    @Override
    public void render(SampledTable node, RenderContext ctx, SqlWriter w) {
        w.append(node.source()).space().append("SAMPLE");
        if (node.sampleSpec().method() == TableSampleSpec.SampleMethod.BLOCK) {
            w.space().append("BLOCK");
        }
        w.space().append("(").append(node.sampleSpec().amount()).append(")");
        if (node.sampleSpec().repeatableSeed() != null) {
            w.space().append("SEED").space().append("(").append(node.sampleSpec().repeatableSeed()).append(")");
        }
        renderTableAlias(node.alias(), null, ctx, w);
    }
}
