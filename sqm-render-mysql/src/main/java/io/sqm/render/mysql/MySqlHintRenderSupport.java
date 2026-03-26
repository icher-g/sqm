package io.sqm.render.mysql;

import io.sqm.core.ExpressionHintArg;
import io.sqm.core.Hint;
import io.sqm.core.IdentifierHintArg;
import io.sqm.core.QualifiedNameHintArg;
import io.sqm.core.StatementHint;
import io.sqm.core.TableHint;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.mysql.spi.MySqlOptimizerHintNormalizationPolicy;
import io.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * Shared MySQL hint rendering helpers.
 */
final class MySqlHintRenderSupport {
    private MySqlHintRenderSupport() {
    }

    static void renderStatementHints(List<StatementHint> hints, String featureName, RenderContext ctx, SqlWriter w) {
        if (hints.isEmpty()) {
            return;
        }

        if (!ctx.dialect().capabilities().supports(SqlFeature.OPTIMIZER_HINT_COMMENT)) {
            throw new UnsupportedDialectFeatureException(featureName, ctx.dialect().name());
        }

        var policy = optimizationPolicy(ctx);
        for (var hint : hints) {
            w.space().append("/*+ ").append(policy.normalize(renderHint(hint, ctx))).append(" */");
        }
    }

    static boolean isIndexHint(TableHint hint) {
        return hint.name().value().matches("^(USE|IGNORE|FORCE)_INDEX(_FOR_(JOIN|ORDER_BY|GROUP_BY))?$");
    }

    static boolean isSqlServerLockHint(TableHint hint) {
        return switch (hint.name().value()) {
            case "NOLOCK", "UPDLOCK", "HOLDLOCK" -> true;
            default -> false;
        };
    }

    static String indexHintKeyword(TableHint hint) {
        var name = hint.name().value();
        if (name.startsWith("USE_")) {
            return "USE";
        }
        if (name.startsWith("IGNORE_")) {
            return "IGNORE";
        }
        if (name.startsWith("FORCE_")) {
            return "FORCE";
        }
        throw new IllegalArgumentException("Unsupported MySQL index hint " + name);
    }

    static String indexHintScope(TableHint hint) {
        return switch (hint.name().value()) {
            case "USE_INDEX_FOR_JOIN", "IGNORE_INDEX_FOR_JOIN", "FORCE_INDEX_FOR_JOIN" -> "JOIN";
            case "USE_INDEX_FOR_ORDER_BY", "IGNORE_INDEX_FOR_ORDER_BY", "FORCE_INDEX_FOR_ORDER_BY" -> "ORDER BY";
            case "USE_INDEX_FOR_GROUP_BY", "IGNORE_INDEX_FOR_GROUP_BY", "FORCE_INDEX_FOR_GROUP_BY" -> "GROUP BY";
            default -> null;
        };
    }

    static void renderHintArgs(Hint hint, RenderContext ctx, SqlWriter w) {
        for (int i = 0; i < hint.args().size(); i++) {
            if (i > 0) {
                w.append(", ");
            }
            var arg = hint.args().get(i);
            switch (arg) {
                case IdentifierHintArg identifierHintArg -> w.append(ctx.dialect().quoter().quoteIfNeeded(identifierHintArg.value().value()));
                case QualifiedNameHintArg qualifiedNameHintArg -> w.append(String.join(".", qualifiedNameHintArg.value().values()));
                case ExpressionHintArg expressionHintArg -> w.append(expressionHintArg.value());
            }
        }
    }

    static String renderHint(Hint hint, RenderContext ctx) {
        var writer = new StringBuilder();
        writer.append(hint.name().value());
        if (!hint.args().isEmpty()) {
            writer.append("(");
            for (int i = 0; i < hint.args().size(); i++) {
                if (i > 0) {
                    writer.append(", ");
                }
                var arg = hint.args().get(i);
                switch (arg) {
                    case IdentifierHintArg identifierHintArg -> writer.append(identifierHintArg.value().value());
                    case QualifiedNameHintArg qualifiedNameHintArg -> writer.append(String.join(".", qualifiedNameHintArg.value().values()));
                    case ExpressionHintArg expressionHintArg -> writer.append(ctx.render(expressionHintArg.value()).sql());
                }
            }
            writer.append(")");
        }
        return writer.toString();
    }

    private static MySqlOptimizerHintNormalizationPolicy optimizationPolicy(RenderContext ctx) {
        if (ctx.dialect() instanceof MySqlDialect mySqlDialect) {
            return mySqlDialect.optimizerHintNormalizationPolicy();
        }
        return MySqlOptimizerHintNormalizationPolicy.PASS_THROUGH;
    }
}
