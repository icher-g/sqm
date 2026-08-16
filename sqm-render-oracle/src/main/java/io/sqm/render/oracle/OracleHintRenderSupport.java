package io.sqm.render.oracle;

import io.sqm.core.ExpressionHintArg;
import io.sqm.core.Hint;
import io.sqm.core.IdentifierHintArg;
import io.sqm.core.QualifiedNameHintArg;
import io.sqm.core.StatementHint;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * Renders Oracle optimizer-hint comments.
 */
final class OracleHintRenderSupport {
    private OracleHintRenderSupport() {
    }

    static void renderStatementHints(List<StatementHint> hints, String featureName, RenderContext ctx, SqlWriter w) {
        if (hints.isEmpty()) {
            return;
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.OPTIMIZER_HINT_COMMENT)) {
            throw new UnsupportedDialectFeatureException(featureName, ctx.dialect().name());
        }

        w.space().append("/*+ ");
        for (int index = 0; index < hints.size(); index++) {
            if (index > 0) {
                w.space();
            }
            w.append(renderHint(hints.get(index), ctx));
        }
        w.append(" */");
    }

    private static String renderHint(Hint hint, RenderContext ctx) {
        var rendered = new StringBuilder(hint.name().value());
        if (hint.args().isEmpty()) {
            return rendered.toString();
        }

        rendered.append('(');
        for (int index = 0; index < hint.args().size(); index++) {
            if (index > 0) {
                rendered.append(", ");
            }
            switch (hint.args().get(index)) {
                case IdentifierHintArg identifierHintArg -> rendered.append(identifierHintArg.value().value());
                case QualifiedNameHintArg qualifiedNameHintArg -> rendered.append(String.join(".", qualifiedNameHintArg.value().values()));
                case ExpressionHintArg expressionHintArg -> rendered.append(ctx.render(expressionHintArg.value()).sql());
            }
        }
        return rendered.append(')').toString();
    }
}
