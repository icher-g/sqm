package io.sqm.transpile.rule;

import io.sqm.core.dialect.SqlDialectId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultTranspileRuleRegistryDefaultsTest {
    @Test
    void defaultsIncludePostgresToMySqlBuiltIns() {
        var rules = DefaultTranspileRuleRegistry.defaults()
            .rulesFor(SqlDialectId.POSTGRESQL, SqlDialectId.MYSQL);

        assertEquals(
            java.util.List.of(
                "postgres-to-mysql-distinct-on-unsupported",
                "postgres-to-mysql-ilike",
                "postgres-to-mysql-null-safe-comparison",
                "postgres-to-mysql-operator-family-unsupported",
                "postgres-to-mysql-regex-variant-unsupported",
                "postgres-to-mysql-returning-unsupported",
                "postgres-to-mysql-similar-to-unsupported"
            ),
            rules.stream().map(TranspileRule::id).sorted().toList()
        );
    }

    @Test
    void defaultsIncludeMySqlToPostgresBuiltIns() {
        var rules = DefaultTranspileRuleRegistry.defaults()
            .rulesFor(SqlDialectId.MYSQL, SqlDialectId.POSTGRESQL);

        assertEquals(
            java.util.List.of(
                "mysql-to-postgres-hint-dropping",
                "mysql-to-postgres-insert-mode-unsupported",
                "mysql-to-postgres-json-function-unsupported",
                "mysql-to-postgres-null-safe-comparison",
                "mysql-to-postgres-on-duplicate-key-unsupported"
            ),
            rules.stream().map(TranspileRule::id).sorted().toList()
        );
    }
}

