package io.sqm.transpile;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.dsl.Dsl;
import io.sqm.transpile.rule.DefaultTranspileRuleRegistry;
import io.sqm.transpile.rule.TranspileRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSqlTranspilerTest {

    @Test
    void transpilesAnsiConcatToMySqlRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.of("ansi"))
            .targetDialect(SqlDialectId.of("mysql"))
            .build();

        var result = transpiler.transpile("SELECT first_name || ' ' || last_name AS full_name FROM users");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesMySqlConcatToPostgreSqlRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.of("mysql"))
            .targetDialect(SqlDialectId.of("postgres"))
            .build();

        var result = transpiler.transpile("SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT first_name || ' ' || last_name AS full_name FROM users"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void validatesAgainstTargetSchemaWhenProvided() {
        var schema = CatalogSchema.of(
            CatalogTable.of(
                "public",
                "users",
                CatalogColumn.of("id", CatalogType.LONG)
            )
        );
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.of("ansi"))
            .targetDialect(SqlDialectId.of("postgresql"))
            .targetSchema(schema)
            .build();

        var result = transpiler.transpile("SELECT name FROM users");

        assertEquals(TranspileStatus.VALIDATION_FAILED, result.status());
        assertFalse(result.sql().isPresent());
        assertFalse(result.problems().isEmpty());
    }

    @Test
    void transpileStatementAppliesConfiguredRulesAndCapturesSteps() {
        Statement statement = Dsl.select(Dsl.col("first_name")).from(Dsl.tbl("users")).build();
        TranspileRule rule = new TranspileRule() {
            @Override
            public String id() {
                return "noop-rule";
            }

            @Override
            public Set<SqlDialectId> sourceDialects() {
                return Set.of(SqlDialectId.of("ansi"));
            }

            @Override
            public Set<SqlDialectId> targetDialects() {
                return Set.of(SqlDialectId.of("mysql"));
            }

            @Override
            public TranspileRuleResult apply(Statement statement, TranspileContext context) {
                return TranspileRuleResult.unchanged(statement, "No-op");
            }
        };

        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.of("ansi"))
            .targetDialect(SqlDialectId.of("mysql"))
            .registry(DefaultTranspileRuleRegistry.of(List.of(rule)))
            .build();

        var result = transpiler.transpile(statement);

        assertTrue(result.success());
        assertEquals(1, result.steps().size());
        assertEquals("noop-rule", result.steps().getFirst().ruleId());
    }

    @Test
    void registryUsesRulePairSupportMethod() {
        Statement statement = Dsl.select(Dsl.col("first_name")).from(Dsl.tbl("users")).build();
        TranspileRule supportedRule = new TranspileRule() {
            @Override
            public String id() {
                return "supported-rule";
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
            public boolean supports(SqlDialectId sourceDialect, SqlDialectId targetDialect) {
                return "ansi".equals(sourceDialect.value()) && "mysql".equals(targetDialect.value());
            }

            @Override
            public TranspileRuleResult apply(Statement statement, TranspileContext context) {
                return TranspileRuleResult.unchanged(statement, "Supported");
            }
        };
        TranspileRule skippedRule = new TranspileRule() {
            @Override
            public String id() {
                return "skipped-rule";
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
            public boolean supports(SqlDialectId sourceDialect, SqlDialectId targetDialect) {
                return false;
            }

            @Override
            public TranspileRuleResult apply(Statement statement, TranspileContext context) {
                throw new AssertionError("Rule should have been filtered out by the registry");
            }
        };

        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.of("ansi"))
            .targetDialect(SqlDialectId.of("mysql"))
            .registry(DefaultTranspileRuleRegistry.of(List.of(skippedRule, supportedRule)))
            .build();

        var result = transpiler.transpile(statement);

        assertTrue(result.success());
        assertEquals(List.of("supported-rule"), result.steps().stream().map(TranspileStep::ruleId).toList());
    }

    @Test
    void buildRequiresConfiguredDialects() {
        assertThrows(IllegalStateException.class, () -> SqlTranspiler.builder()
            .targetDialect(SqlDialectId.of("mysql"))
            .build());
        assertThrows(IllegalStateException.class, () -> SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.of("ansi"))
            .build());
    }

    private static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
