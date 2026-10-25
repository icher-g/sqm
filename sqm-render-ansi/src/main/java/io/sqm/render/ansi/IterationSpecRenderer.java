package io.sqm.render.ansi;

import io.sqm.core.IterationSpec;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for IterationSpec. Oracle support follows in #506.
 */
public class IterationSpecRenderer implements Renderer<IterationSpec> {
    /**
     * Creates the IterationSpec render.
     */
    public IterationSpecRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(IterationSpec node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException(ctx.dialect().capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL IterationSpec is not supported yet (#506)" : "MODEL IterationSpec is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<IterationSpec> targetType() {
        return IterationSpec.class;
    }
}
