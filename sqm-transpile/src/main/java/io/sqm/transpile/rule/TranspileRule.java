package io.sqm.transpile.rule;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;

import java.util.Set;

/**
 * Source-to-target transpilation rule.
 */
public interface TranspileRule {
    /**
     * Returns a stable rule identifier.
     *
     * @return rule identifier
     */
    String id();

    /**
     * Returns supported source dialects.
     *
     * @return supported source dialects, or an empty set for runtime-only applicability
     */
    Set<SqlDialectId> sourceDialects();

    /**
     * Returns supported target dialects.
     *
     * @return supported target dialects, or an empty set for runtime-only applicability
     */
    Set<SqlDialectId> targetDialects();

    /**
     * Returns execution order for the rule.
     *
     * @return rule execution order
     */
    default int order() {
        return 0;
    }

    /**
     * Returns whether the rule supports the provided source and target dialect pair.
     *
     * @param sourceDialect source dialect identifier
     * @param targetDialect target dialect identifier
     * @return {@code true} when the rule can run for the dialect pair
     */
    default boolean supports(SqlDialectId sourceDialect, SqlDialectId targetDialect) {
        boolean sourceSupported = sourceDialects().isEmpty() || sourceDialects().contains(sourceDialect);
        boolean targetSupported = targetDialects().isEmpty() || targetDialects().contains(targetDialect);
        return sourceSupported && targetSupported;
    }

    /**
     * Returns whether the rule supports the current transpilation request.
     *
     * @param context transpilation context
     * @return {@code true} when the rule can run
     */
    default boolean supports(TranspileContext context) {
        return supports(context.sourceDialect(), context.targetDialect());
    }

    /**
     * Applies the rule to the current statement.
     *
     * @param statement current statement
     * @param context transpilation context
     * @return rule result
     */
    TranspileRuleResult apply(Statement statement, TranspileContext context);
}
