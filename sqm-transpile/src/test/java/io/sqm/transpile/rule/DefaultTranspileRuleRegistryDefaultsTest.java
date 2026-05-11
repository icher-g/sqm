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
                "function-table-to-mysql-unsupported",
                "postgres-merge-unsupported",
                "postgres-to-mysql-distinct-on-unsupported",
                "postgres-to-mysql-ilike",
                "postgres-to-mysql-null-safe-comparison",
                "postgres-to-mysql-operator-family-unsupported",
                "postgres-to-mysql-regex-variant-unsupported",
                "postgres-to-mysql-returning-unsupported",
                "postgres-to-mysql-similar-to-unsupported",
                "sequence-value-unsupported"
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
                "mysql-hint-dropping",
                "mysql-to-postgres-insert-mode-unsupported",
                "mysql-to-postgres-json-function-unsupported",
                "mysql-to-postgres-null-safe-comparison",
                "mysql-to-postgres-on-duplicate-key-unsupported"
            ),
            rules.stream().map(TranspileRule::id).sorted().toList()
        );
    }

    @Test
    void defaultsIncludeSqlServerBuiltIns() {
        var postgresToSqlServerRules = DefaultTranspileRuleRegistry.defaults()
            .rulesFor(SqlDialectId.POSTGRESQL, SqlDialectId.SQLSERVER);
        var sqlServerToPostgresRules = DefaultTranspileRuleRegistry.defaults()
            .rulesFor(SqlDialectId.SQLSERVER, SqlDialectId.POSTGRESQL);
        var ansiToSqlServerRules = DefaultTranspileRuleRegistry.defaults()
            .rulesFor(SqlDialectId.ANSI, SqlDialectId.SQLSERVER);

        assertEquals(
            java.util.List.of(
                "hierarchical-query-unsupported",
                "postgres-merge-do-nothing-unsupported",
                "postgres-to-sqlserver-distinct-on-unsupported",
                "postgres-to-sqlserver-returning-unsupported",
                "sequence-value-unsupported",
                "standard-limit-to-sqlserver-top"
            ),
            postgresToSqlServerRules.stream().map(TranspileRule::id).sorted().toList()
        );
        assertEquals(
            java.util.List.of(
                "pivot-unpivot-approximate-rewrite",
                "sqlserver-hint-dropping",
                "sqlserver-merge-unsupported",
                "sqlserver-output-unsupported",
                "sqlserver-top-to-limit"
            ),
            sqlServerToPostgresRules.stream().map(TranspileRule::id).sorted().toList()
        );
        assertEquals(
            java.util.List.of("hierarchical-query-unsupported", "sequence-value-unsupported", "standard-limit-to-sqlserver-top"),
            ansiToSqlServerRules.stream().map(TranspileRule::id).sorted().toList()
        );
    }

    @Test
    void defaultsIncludeOracleBuiltIns() {
        var postgresToOracleRules = DefaultTranspileRuleRegistry.defaults()
            .rulesFor(SqlDialectId.POSTGRESQL, SqlDialectId.ORACLE);
        var oracleToPostgresRules = DefaultTranspileRuleRegistry.defaults()
            .rulesFor(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL);
        var sqlServerToOracleRules = DefaultTranspileRuleRegistry.defaults()
            .rulesFor(SqlDialectId.SQLSERVER, SqlDialectId.ORACLE);
        var oracleToSqlServerRules = DefaultTranspileRuleRegistry.defaults()
            .rulesFor(SqlDialectId.ORACLE, SqlDialectId.SQLSERVER);

        assertEquals(
            java.util.List.of(
                "oracle-result-clause-unsupported",
                "postgres-merge-do-nothing-unsupported",
                "postgres-merge-not-matched-by-source-to-oracle-unsupported",
                "postgres-to-oracle-distinct-on-unsupported"
            ),
            postgresToOracleRules.stream().map(TranspileRule::id).sorted().toList()
        );
        assertEquals(
            java.util.List.of(
                "hierarchical-query-to-recursive-cte",
                "oracle-hint-dropping",
                "oracle-returning-into-unsupported",
                "pivot-unpivot-approximate-rewrite"
            ),
            oracleToPostgresRules.stream().map(TranspileRule::id).sorted().toList()
        );
        assertEquals(
            java.util.List.of(
                "oracle-result-clause-unsupported",
                "sqlserver-hint-dropping",
                "sqlserver-merge-unsupported",
                "sqlserver-output-unsupported",
                "sqlserver-top-to-limit"
            ),
            sqlServerToOracleRules.stream().map(TranspileRule::id).sorted().toList()
        );
        assertEquals(
            java.util.List.of(
                "hierarchical-query-unsupported",
                "oracle-hint-dropping",
                "oracle-returning-into-unsupported",
                "sequence-value-unsupported",
                "standard-limit-to-sqlserver-top"
            ),
            oracleToSqlServerRules.stream().map(TranspileRule::id).sorted().toList()
        );
    }
}

