package io.sqm.examples;

import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.SqlTranspiler;

/**
 * Demonstrates exact PostgreSQL-to-MySQL SQL transpilation.
 */
public final class Transpile_PostgresToMySql {
    /**
     * Runs the example.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile(
            "SELECT first_name || ' ' || last_name AS full_name FROM users"
        );

        System.out.println("=== Status ===");
        System.out.println(result.status());
        System.out.println("=== MySQL SQL ===");
        System.out.println(result.sql().orElse("<no sql>"));
    }
}

