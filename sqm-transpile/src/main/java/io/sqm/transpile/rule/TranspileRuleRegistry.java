package io.sqm.transpile.rule;

import io.sqm.core.dialect.SqlDialectId;

import java.util.List;

/**
 * Registry of transpilation rules for a source and target dialect pair.
 */
public interface TranspileRuleRegistry {
    /**
     * Returns ordered rules for the provided source and target dialects.
     *
     * @param source source dialect
     * @param target target dialect
     * @return ordered transpilation rules
     */
    List<TranspileRule> rulesFor(SqlDialectId source, SqlDialectId target);
}
