package io.sqm.it;

import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.SqlTranspiler;
import io.sqm.transpile.TranspileOptions;
import io.sqm.transpile.TranspileStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlTranspilerIntegrationTest {

    @Test
    void transpilesPostgresConcatQueryToMySqlSqlText() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile(
            "SELECT first_name || ' ' || last_name AS full_name FROM users"
        );

        assertEquals(TranspileStatus.SUCCESS, result.status());
        assertEquals(
            Utils.normalizeSql("SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users"),
            Utils.normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesPostgresIlikeQueryToMySqlSqlTextWithWarningWhenEnabled() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .options(new TranspileOptions(true, false, true, true))
            .build();

        var result = transpiler.transpile(
            "SELECT * FROM users WHERE name ILIKE 'al%'"
        );

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        assertEquals(
            Utils.normalizeSql("SELECT * FROM users WHERE LOWER(name) LIKE LOWER('al%')"),
            Utils.normalizeSql(result.sql().orElseThrow())
        );
        assertEquals("APPROXIMATE_ILIKE_LOWERING", result.warnings().getFirst().code());
    }
}

