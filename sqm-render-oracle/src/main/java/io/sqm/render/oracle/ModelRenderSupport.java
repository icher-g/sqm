package io.sqm.render.oracle;

import io.sqm.core.RuleMode;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.spi.RenderContext;

/**
 * Common MODEL capability checks and option spelling.
 */
final class ModelRenderSupport {
    private ModelRenderSupport() {
    }

    static void require(RenderContext ctx) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.MODEL_CLAUSE)) {
            throw new UnsupportedDialectFeatureException(SqlFeature.MODEL_CLAUSE.description(), ctx.dialect().name());
        }
    }

    static String mode(RuleMode mode) {
        return switch (mode) {
            case UPDATE -> "UPDATE";
            case UPSERT -> "UPSERT";
            case UPSERT_ALL -> "UPSERT ALL";
        };
    }
}
