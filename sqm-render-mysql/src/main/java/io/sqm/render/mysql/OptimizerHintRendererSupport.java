package io.sqm.render.mysql;

import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.mysql.spi.MySqlOptimizerHintNormalizationPolicy;
import io.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * Shared helper for rendering MySQL optimizer hints with optional
 * normalization.
 */
final class OptimizerHintRendererSupport {
    private OptimizerHintRendererSupport() {
    }

    static void renderHints(List<String> hints, String featureName, RenderContext ctx, SqlWriter w) {
        if (hints.isEmpty()) {
            return;
        }

        if (!ctx.dialect().capabilities().supports(SqlFeature.OPTIMIZER_HINT_COMMENT)) {
            throw new UnsupportedDialectFeatureException(featureName, ctx.dialect().name());
        }

        var policy = optimizationPolicy(ctx);
        for (var hint : hints) {
            w.space().append("/*+ ").append(policy.normalize(hint)).append(" */");
        }
    }

    private static MySqlOptimizerHintNormalizationPolicy optimizationPolicy(RenderContext ctx) {
        if (ctx.dialect() instanceof MySqlDialect mySqlDialect) {
            return mySqlDialect.optimizerHintNormalizationPolicy();
        }
        return MySqlOptimizerHintNormalizationPolicy.PASS_THROUGH;
    }
}
