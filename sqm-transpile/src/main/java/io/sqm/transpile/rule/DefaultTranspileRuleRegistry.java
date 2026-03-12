package io.sqm.transpile.rule;

import io.sqm.core.dialect.SqlDialectId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Default reusable rule registry implementation.
 */
public final class DefaultTranspileRuleRegistry implements TranspileRuleRegistry {
    private final List<TranspileRule> rules;

    private DefaultTranspileRuleRegistry(List<TranspileRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Returns an empty registry.
     *
     * @return empty rule registry
     */
    public static DefaultTranspileRuleRegistry defaults() {
        return new DefaultTranspileRuleRegistry(List.of());
    }

    /**
     * Returns a registry backed by the provided rules.
     *
     * @param rules transpilation rules
     * @return rule registry
     */
    public static DefaultTranspileRuleRegistry of(List<TranspileRule> rules) {
        Objects.requireNonNull(rules, "rules");
        return new DefaultTranspileRuleRegistry(rules);
    }

    /**
     * Returns a registry backed by the provided rules.
     *
     * @param rules transpilation rules
     * @return rule registry
     */
    public static DefaultTranspileRuleRegistry of(TranspileRule... rules) {
        return of(List.of(rules));
    }

    @Override
    public List<TranspileRule> rulesFor(SqlDialectId source, SqlDialectId target) {
        var selected = new ArrayList<TranspileRule>();
        for (var rule : rules) {
            if (rule.supports(source, target)) {
                selected.add(rule);
            }
        }
        selected.sort(Comparator.comparingInt(TranspileRule::order).thenComparing(TranspileRule::id));
        return List.copyOf(selected);
    }
}
