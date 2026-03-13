package io.sqm.transpile.builtin;

import io.sqm.core.RegexMode;
import io.sqm.core.RegexPredicate;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Rejects PostgreSQL regex variants that do not have an exact MySQL mapping in the current slice.
 */
public final class PostgresToMySqlRegexVariantUnsupportedRule implements TranspileRule {
    /**
     * Creates a PostgreSQL-to-MySQL regex-variant rejection rule.
     */
    public PostgresToMySqlRegexVariantUnsupportedRule() {
    }

    @Override
    public String id() {
        return "postgres-to-mysql-regex-variant-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.of("postgresql"));
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.of("mysql"));
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!hasUnsupportedRegexVariant(statement)) {
            return TranspileRuleResult.unchanged(statement, "No unsupported PostgreSQL regex variant detected");
        }
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_REGEX_VARIANT",
            "PostgreSQL case-insensitive regex variants are not supported for exact MySQL transpilation"
        );
    }

    private static boolean hasUnsupportedRegexVariant(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitRegexPredicate(RegexPredicate predicate) {
                if (predicate.mode() != RegexMode.MATCH) {
                    found.set(true);
                }
                return super.visitRegexPredicate(predicate);
            }
        });
        return found.get();
    }
}
