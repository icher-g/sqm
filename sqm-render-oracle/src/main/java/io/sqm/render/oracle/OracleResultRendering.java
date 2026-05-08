package io.sqm.render.oracle;

import io.sqm.core.ResultClause;
import io.sqm.core.VariableResultTarget;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

final class OracleResultRendering {
    private OracleResultRendering() {
    }

    static void renderReturningInto(String feature, ResultClause result, RenderContext ctx, SqlWriter w) {
        if (result == null || result.items().isEmpty()) {
            return;
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.DML_RESULT_CLAUSE)
            || !ctx.dialect().capabilities().supports(SqlFeature.DML_RESULT_VARIABLE_TARGET)) {
            throw new UnsupportedDialectFeatureException(feature, ctx.dialect().name());
        }
        if (!(result.target() instanceof VariableResultTarget target)) {
            throw new UnsupportedDialectFeatureException(feature + " without INTO variables", ctx.dialect().name());
        }
        w.newline().append("RETURNING").space().comma(result.items());
        w.space().append("INTO").space().append(target);
    }
}
