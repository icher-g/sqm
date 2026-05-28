package io.sqm.render.postgresql;

import io.sqm.core.SampledTable;
import io.sqm.core.TableSampleSpec;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders PostgreSQL sampled table references.
 */
public class SampledTableRenderer extends io.sqm.render.ansi.SampledTableRenderer {
    /**
     * Creates a PostgreSQL sampled table renderer.
     */
    public SampledTableRenderer() {
    }

    @Override
    public void render(SampledTable node, RenderContext ctx, SqlWriter w) {
        if (node.sampleSpec().method() != TableSampleSpec.SampleMethod.BERNOULLI
            && node.sampleSpec().method() != TableSampleSpec.SampleMethod.SYSTEM) {
            throw new UnsupportedDialectFeatureException("PostgreSQL table sample method " + node.sampleSpec().method(), ctx.dialect().name());
        }
        w.append(node.source()).space().append("TABLESAMPLE").space().append(node.sampleSpec().method().name()).space();
        w.append("(").append(node.sampleSpec().amount()).append(")");
        if (node.sampleSpec().repeatableSeed() != null) {
            w.space().append("REPEATABLE").space().append("(").append(node.sampleSpec().repeatableSeed()).append(")");
        }
        if (node.alias() != null) {
            w.space().append("AS").space().append(renderIdentifier(node.alias(), ctx.dialect().quoter()));
        }
    }
}
