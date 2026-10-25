package io.sqm.render.ansi;

import io.sqm.core.ReferenceModel;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Explicit unsupported boundary for ReferenceModel. Oracle support follows in #506.
 */
public class ReferenceModelRenderer implements Renderer<ReferenceModel> {
    /**
     * Creates the ReferenceModel render.
     */
    public ReferenceModelRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ReferenceModel node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException(ctx.dialect().capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL ReferenceModel is not supported yet (#506)" : "MODEL ReferenceModel is not supported by this dialect", ctx.dialect().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ReferenceModel> targetType() {
        return ReferenceModel.class;
    }
}
