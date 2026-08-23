package io.sqm.transpile.builtin;

import io.sqm.core.PatternRecognitionTable;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Rejects row-pattern recognition unless a configured compatibility check proves
 * that the complete modeled relation is preserved exactly.
 */
public final class MatchRecognizeUnsupportedRule implements TranspileRule {
    private final BiPredicate<PatternRecognitionTable, TranspileContext> exactCompatibility;

    /**
     * Creates the default rule, which recognizes only Oracle-to-Oracle native
     * preservation as exact.
     */
    public MatchRecognizeUnsupportedRule() {
        this((table, context) ->
            SqlDialectId.ORACLE.equals(context.sourceDialect())
                && SqlDialectId.ORACLE.equals(context.targetDialect()));
    }

    /**
     * Creates a rule with a compatibility hook for dialect integrations that can
     * prove exact support for every modeled row-pattern option.
     *
     * @param exactCompatibility predicate receiving the relation and transpilation context
     */
    public MatchRecognizeUnsupportedRule(
        BiPredicate<PatternRecognitionTable, TranspileContext> exactCompatibility
    ) {
        this.exactCompatibility = Objects.requireNonNull(exactCompatibility, "exactCompatibility");
    }

    @Override
    public String id() {
        return "match-recognize-unsupported";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of();
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of();
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        var relations = StatementFeatureInspector.patternRecognitionTables(statement);
        if (relations.isEmpty()) {
            return TranspileRuleResult.unchanged(statement, "No MATCH_RECOGNIZE relation detected");
        }

        var unsupported = relations.stream()
            .filter(located -> !exactCompatibility.test(located.table(), context))
            .findFirst();
        if (unsupported.isEmpty()) {
            return TranspileRuleResult.unchanged(statement, "MATCH_RECOGNIZE is preserved exactly");
        }

        var located = unsupported.orElseThrow();
        var message = "MATCH_RECOGNIZE at " + located.path()
            + " has no exact transpilation from " + context.sourceDialect().value()
            + " to " + context.targetDialect().value();
        return TranspileRuleResult.unsupported(
            statement,
            "UNSUPPORTED_MATCH_RECOGNIZE",
            message,
            located.path()
        );
    }
}
