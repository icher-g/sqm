package io.sqm.examples;

import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.SqlTranspiler;
import io.sqm.transpile.TranspileOptions;

/**
 * Demonstrates approximate PostgreSQL-to-MySQL SQL transpilation with warnings.
 */
public final class Transpile_PostgresToMySqlApproximate {
    /**
     * Runs the example.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.of("postgresql"))
            .targetDialect(SqlDialectId.of("mysql"))
            .options(new TranspileOptions(true, false, true, true))
            .build();

        var result = transpiler.transpile(
            "SELECT * FROM users WHERE name ILIKE 'al%'"
        );

        System.out.println("=== Status ===");
        System.out.println(result.status());
        System.out.println("=== MySQL SQL ===");
        System.out.println(result.sql().orElse("<no sql>"));
        System.out.println("=== Warnings ===");
        result.warnings().forEach(warning ->
            System.out.println(warning.code() + ": " + warning.message())
        );
    }
}
