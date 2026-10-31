package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.oracle.dialect.OracleCapabilities;
import io.sqm.transpile.*;
import io.sqm.transpile.rule.TranspileRule;

import java.util.List;
import java.util.Set;

/**
 * Preserves MODEL only when exact native compatibility is established.
 * Empty dialect sets deliberately make this a runtime guard for every dialect pair.
 * MODEL is never approximated with relational or procedural rewrites.
 */
public final class ModelClauseUnsupportedRule implements TranspileRule {
    private final DialectCapabilities targetCapabilities;

    /**
     * Creates the native Oracle-to-Oracle guard with the latest supported Oracle capabilities.
     * Supply explicit capabilities when the target renderer has a different feature set.
     */
    public ModelClauseUnsupportedRule() {
        this(OracleCapabilities.latest());
    }

    /**
     * Creates a guard for the configured target capabilities. Native MODEL preservation
     * requires an Oracle-to-Oracle pair and target support for the complete clause.
     *
     * @param targetCapabilities configured target dialect capabilities
     */
    public ModelClauseUnsupportedRule(DialectCapabilities targetCapabilities) {
        this.targetCapabilities = targetCapabilities != null ? targetCapabilities : OracleCapabilities.latest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return "model-clause-unsupported";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preflight() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        var clauses = StatementFeatureInspector.modelClauses(statement);
        var problems = clauses.stream().filter(located -> !compatible(context))
            .map(located -> new TranspileProblem("UNSUPPORTED_MODEL_CLAUSE",
                "MODEL at " + located.path() + " has no exact transpilation from "
                    + context.sourceDialect().value() + " to " + context.targetDialect().value(),
                TranspileStage.REWRITE, null, null, null, null, located.path())
                .withDialects(context.sourceDialect(), context.targetDialect())).toList();
        if (problems.isEmpty()) return TranspileRuleResult.unchanged(statement, "MODEL is preserved exactly or absent");
        return new TranspileRuleResult(statement, false, RewriteFidelity.UNSUPPORTED, List.of(), problems,
            "MODEL cannot be represented exactly by the target dialect");
    }

    private boolean compatible(TranspileContext context) {
        return SqlDialectId.ORACLE.equals(context.sourceDialect())
            && SqlDialectId.ORACLE.equals(context.targetDialect())
            && targetCapabilities.supports(SqlFeature.MODEL_CLAUSE);
    }
}
