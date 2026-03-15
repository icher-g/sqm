package io.sqm.transpile;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.Node;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.dsl.Dsl;
import io.sqm.parser.spi.Specs;
import io.sqm.render.spi.PreparedNode;
import io.sqm.transpile.rule.DefaultTranspileRuleRegistry;
import io.sqm.transpile.rule.TranspileRule;
import io.sqm.validate.schema.SchemaValidationSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSqlTranspilerTest {

    @Test
    void transpilesAnsiConcatToMySqlRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ANSI)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT first_name || ' ' || last_name AS full_name FROM users");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesPostgresConcatToMySqlRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT first_name || ' ' || last_name AS full_name FROM users");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesPostgresRegexPredicateToMySqlRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE name ~ '^al.*'");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT * FROM users WHERE name REGEXP '^al.*'"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesNegatedPostgresRegexPredicateToMySqlRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE name !~ '^al.*'");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT * FROM users WHERE name NOT REGEXP '^al.*'"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesPostgresIsNotDistinctFromToMySqlNullSafeEquality() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE first_name IS NOT DISTINCT FROM last_name");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT * FROM users WHERE first_name <=> last_name"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesPostgresIsDistinctFromToNegatedMySqlNullSafeEquality() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE first_name IS DISTINCT FROM last_name");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT * FROM users WHERE NOT (first_name <=> last_name)"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesMySqlConcatToPostgreSqlRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT first_name || ' ' || last_name AS full_name FROM users"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesAnsiLimitToSqlServerTopRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ANSI)
            .targetDialect(SqlDialectId.SQLSERVER)
            .build();

        var result = transpiler.transpile("SELECT id FROM users LIMIT 5");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT TOP (5) id FROM users"),
            normalizeSql(result.sql().orElseThrow())
        );
        assertTrue(result.steps().stream().anyMatch(step ->
            "standard-limit-to-sqlserver-top".equals(step.ruleId())
                && step.fidelity() == RewriteFidelity.EXACT
        ));
    }

    @Test
    void transpilesSqlServerTopToPostgresLimitRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.SQLSERVER)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("SELECT TOP (5) id FROM users");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT id FROM users LIMIT 5"),
            normalizeSql(result.sql().orElseThrow())
        );
        assertTrue(result.steps().stream().anyMatch(step ->
            "sqlserver-top-to-limit".equals(step.ruleId())
                && step.fidelity() == RewriteFidelity.EXACT
        ));
    }

    @Test
    void transpilesMySqlRegexPredicateToPostgresRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE name REGEXP '^al.*'");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT * FROM users WHERE name ~ '^al.*'"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesMySqlNullSafeEqualityToPostgresDistinctness() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE first_name <=> last_name");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT * FROM users WHERE first_name IS NOT DISTINCT FROM last_name"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesNegatedMySqlNullSafeEqualityToPostgresDistinctness() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE NOT (first_name <=> last_name)");

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT * FROM users WHERE first_name IS DISTINCT FROM last_name"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesMySqlNullSafeEqualityInsideJoinPredicate() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile(
            "SELECT * FROM users u JOIN profiles p ON u.profile_id <=> p.id"
        );

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT * FROM users AS u INNER JOIN profiles AS p ON u.profile_id IS NOT DISTINCT FROM p.id"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void transpilesNegatedMySqlNullSafeEqualityInsideCompoundPredicate() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile(
            "SELECT * FROM users WHERE active = TRUE AND NOT (first_name <=> last_name)"
        );

        assertTrue(result.success());
        assertEquals(
            normalizeSql("SELECT * FROM users WHERE active = TRUE AND first_name IS DISTINCT FROM last_name"),
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
            .sourceDialect(SqlDialectId.ANSI)
            .targetDialect(SqlDialectId.POSTGRESQL)
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
                return Set.of(SqlDialectId.ANSI);
            }

            @Override
            public Set<SqlDialectId> targetDialects() {
                return Set.of(SqlDialectId.MYSQL);
            }

            @Override
            public TranspileRuleResult apply(Statement statement, TranspileContext context) {
                return TranspileRuleResult.unchanged(statement, "No-op");
            }
        };

        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ANSI)
            .targetDialect(SqlDialectId.MYSQL)
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
            .sourceDialect(SqlDialectId.ANSI)
            .targetDialect(SqlDialectId.MYSQL)
            .registry(DefaultTranspileRuleRegistry.of(List.of(skippedRule, supportedRule)))
            .build();

        var result = transpiler.transpile(statement);

        assertTrue(result.success());
        assertEquals(List.of("supported-rule"), result.steps().stream().map(TranspileStep::ruleId).toList());
    }

    @Test
    void buildRequiresConfiguredDialects() {
        assertThrows(IllegalStateException.class, () -> SqlTranspiler.builder()
            .targetDialect(SqlDialectId.MYSQL)
            .build());
        assertThrows(IllegalStateException.class, () -> SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ANSI)
            .build());
    }

    @Test
    void parseFailureReturnsParseFailedStatus() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ANSI)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT FROM");

        assertEquals(TranspileStatus.PARSE_FAILED, result.status());
        assertFalse(result.success());
        assertFalse(result.problems().isEmpty());
    }

    @Test
    void approximateRewriteIsRejectedWhenDisabled() {
        var statement = Dsl.select(Dsl.col("first_name")).from(Dsl.tbl("users")).build();
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ANSI)
            .targetDialect(SqlDialectId.MYSQL)
            .registry(DefaultTranspileRuleRegistry.of(approximateRule()))
            .build();

        var result = transpiler.transpile(statement);

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("APPROXIMATE_REWRITE_DISABLED", result.problems().getLast().code());
    }

    @Test
    void warningsCanFailTranspilationWhenConfigured() {
        var statement = Dsl.select(Dsl.col("first_name")).from(Dsl.tbl("users")).build();
        TranspileRule warningRule = new TranspileRule() {
            @Override
            public String id() {
                return "warning-rule";
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
                return new TranspileRuleResult(
                    statement,
                    false,
                    RewriteFidelity.EXACT,
                    List.of(new TranspileWarning("WARN", "Heads up")),
                    List.of(),
                    "Warning"
                );
            }
        };

        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ANSI)
            .targetDialect(SqlDialectId.MYSQL)
            .options(new TranspileOptions(false, true, true, true))
            .registry(DefaultTranspileRuleRegistry.of(warningRule))
            .build();

        var result = transpiler.transpile(statement);

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("WARNINGS_NOT_ALLOWED", result.problems().getFirst().code());
    }

    @Test
    void postgresIlikeCanBeLoweredApproximatelyWhenEnabled() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .options(new TranspileOptions(true, false, true, true))
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE name ILIKE 'al%'");

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        assertEquals(
            normalizeSql("SELECT * FROM users WHERE LOWER(name) LIKE LOWER('al%')"),
            normalizeSql(result.sql().orElseThrow())
        );
        assertEquals("APPROXIMATE_ILIKE_LOWERING", result.warnings().getFirst().code());
    }

    @Test
    void postgresNotIlikeCanBeLoweredApproximatelyWhenEnabled() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .options(new TranspileOptions(true, false, true, true))
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE name NOT ILIKE 'al%'");

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        assertEquals(
            normalizeSql("SELECT * FROM users WHERE LOWER(name) NOT LIKE LOWER('al%')"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void postgresIlikeEscapeIsPreservedInApproximateRewrite() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .options(new TranspileOptions(true, false, true, true))
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE name ILIKE 'a!_%' ESCAPE '!'");

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        assertEquals(
            normalizeSql("SELECT * FROM users WHERE LOWER(name) LIKE LOWER('a!_%') ESCAPE '!'"),
            normalizeSql(result.sql().orElseThrow())
        );
    }

    @Test
    void postgresIlikeIsRejectedByDefaultApproximatePolicy() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE name ILIKE 'al%'");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("APPROXIMATE_REWRITE_DISABLED", result.problems().getLast().code());
    }

    @Test
    void renderCanBeSkippedByOptions() {
        var statement = Dsl.select(Dsl.col("first_name")).from(Dsl.tbl("users")).build();
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ANSI)
            .targetDialect(SqlDialectId.MYSQL)
            .options(new TranspileOptions(false, false, true, false))
            .build();

        var result = transpiler.transpile(statement);

        assertTrue(result.success());
        assertTrue(result.transpiledAst().isPresent());
        assertTrue(result.sql().isEmpty());
    }

    @Test
    void renderFailureIsReported() {
        var statement = Dsl.select(Dsl.col("first_name")).from(Dsl.tbl("users")).build();
        var failingDialect = new io.sqm.render.mysql.spi.MySqlDialect() {
            @Override
            public PreparedNode beforeRender(Node root, io.sqm.render.spi.RenderOptions options) {
                throw new UnsupportedOperationException("Boom");
            }
        };

        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ANSI)
            .targetDialect(SqlDialectId.MYSQL)
            .renderer(() -> failingDialect)
            .build();

        var result = transpiler.transpile(statement);

        assertEquals(TranspileStatus.RENDER_FAILED, result.status());
        assertEquals("RENDER_FAILED", result.problems().getFirst().code());
    }

    @Test
    void customFactoriesAreUsed() {
        AtomicBoolean parserUsed = new AtomicBoolean(false);
        AtomicBoolean rendererUsed = new AtomicBoolean(false);
        AtomicBoolean validationUsed = new AtomicBoolean(false);

        Specs specs = new io.sqm.parser.ansi.AnsiSpecs() {
        };
        var dialect = new io.sqm.render.mysql.spi.MySqlDialect();
        SchemaValidationSettings settings = SchemaValidationSettings.defaults();

        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ANSI)
            .targetDialect(SqlDialectId.MYSQL)
            .parser(() -> {
                parserUsed.set(true);
                return specs;
            })
            .renderer(() -> {
                rendererUsed.set(true);
                return dialect;
            })
            .targetValidation(() -> {
                validationUsed.set(true);
                return settings;
            })
            .targetSchema(CatalogSchema.of(
                CatalogTable.of("public", "users", CatalogColumn.of("first_name", CatalogType.STRING))
            ))
            .build();

        var result = transpiler.transpile("SELECT first_name FROM users");

        assertTrue(result.success());
        assertTrue(parserUsed.get());
        assertTrue(rendererUsed.get());
        assertTrue(validationUsed.get());
    }

    @Test
    void transpileResultContainsSourceAndTranspiledAstOnSuccess() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.ANSI)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT first_name FROM users");

        assertTrue(result.success());
        assertNotNull(result.sourceAst().orElse(null));
        assertNotNull(result.transpiledAst().orElse(null));
    }

    @Test
    void postgresReturningIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("INSERT INTO users (name) VALUES ('alice') RETURNING id");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertTrue(result.sql().isEmpty());
        assertEquals("UNSUPPORTED_RETURNING", result.problems().getFirst().code());
        assertTrue(result.steps().stream().anyMatch(step ->
            "postgres-to-mysql-returning-unsupported".equals(step.ruleId())
                && step.fidelity() == RewriteFidelity.UNSUPPORTED
        ));
    }

    @Test
    void postgresReturningInsideWritableCteIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile(
            "WITH ins AS (INSERT INTO users (name) VALUES ('alice') RETURNING id) SELECT id FROM ins"
        );

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("UNSUPPORTED_RETURNING", result.problems().getFirst().code());
    }

    @Test
    void postgresDistinctOnIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile(
            "SELECT DISTINCT ON (user_id) user_id, created_at FROM orders ORDER BY user_id, created_at DESC"
        );

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertTrue(result.sql().isEmpty());
        assertEquals("UNSUPPORTED_DISTINCT_ON", result.problems().getFirst().code());
        assertTrue(result.steps().stream().anyMatch(step ->
            "postgres-to-mysql-distinct-on-unsupported".equals(step.ruleId())
                && step.fidelity() == RewriteFidelity.UNSUPPORTED
        ));
    }

    @Test
    void postgresDistinctOnIsRejectedBeforeSqlServerRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.SQLSERVER)
            .build();

        var result = transpiler.transpile(
            "SELECT DISTINCT ON (user_id) user_id, created_at FROM orders ORDER BY user_id, created_at DESC"
        );

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertTrue(result.sql().isEmpty());
        assertEquals("UNSUPPORTED_DISTINCT_ON", result.problems().getFirst().code());
        assertTrue(result.steps().stream().anyMatch(step ->
            "postgres-to-sqlserver-distinct-on-unsupported".equals(step.ruleId())
                && step.fidelity() == RewriteFidelity.UNSUPPORTED
        ));
    }

    @Test
    void postgresSimilarToIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE name SIMILAR TO 'a.*'");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("UNSUPPORTED_SIMILAR_TO", result.problems().getFirst().code());
    }

    @Test
    void postgresInsensitiveRegexVariantIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE name ~* '^al.*'");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("UNSUPPORTED_REGEX_VARIANT", result.problems().getFirst().code());
    }

    @Test
    void postgresNegatedInsensitiveRegexVariantIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE name !~* '^al.*'");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("UNSUPPORTED_REGEX_VARIANT", result.problems().getFirst().code());
    }

    @Test
    void postgresJsonOperatorFamilyIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT payload ->> 'name' FROM users");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("UNSUPPORTED_POSTGRES_OPERATOR_FAMILY", result.problems().getFirst().code());
    }

    @Test
    void postgresArrayOverlapOperatorFamilyIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.POSTGRESQL)
            .targetDialect(SqlDialectId.MYSQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users WHERE tags && other_tags");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("UNSUPPORTED_POSTGRES_OPERATOR_FAMILY", result.problems().getFirst().code());
    }

    @Test
    void mysqlOptimizerHintIsDroppedWithWarningBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("SELECT /*+ NO_RANGE_OPTIMIZATION(users) */ * FROM users");

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        assertEquals(normalizeSql("SELECT * FROM users"), normalizeSql(result.sql().orElseThrow()));
        assertEquals("MYSQL_HINTS_DROPPED", result.warnings().getFirst().code());
    }

    @Test
    void mysqlLeadingOptimizerHintIsDroppedWithWarningBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("/*+ MAX_EXECUTION_TIME(1000) */ SELECT id FROM users");

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        assertEquals(normalizeSql("SELECT id FROM users"), normalizeSql(result.sql().orElseThrow()));
        assertEquals("MYSQL_HINTS_DROPPED", result.warnings().getFirst().code());
    }

    @Test
    void mysqlIndexHintIsDroppedWithWarningBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users USE INDEX (idx_users_name)");

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        assertEquals(normalizeSql("SELECT * FROM users"), normalizeSql(result.sql().orElseThrow()));
        assertEquals("MYSQL_HINTS_DROPPED", result.warnings().getFirst().code());
    }

    @Test
    void mysqlForceIndexHintIsDroppedWithWarningBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users FORCE INDEX FOR JOIN (idx_users_name)");

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        assertEquals(normalizeSql("SELECT * FROM users"), normalizeSql(result.sql().orElseThrow()));
        assertEquals("MYSQL_HINTS_DROPPED", result.warnings().getFirst().code());
    }

    @Test
    void mysqlIgnoreIndexHintIsDroppedWithWarningBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("SELECT * FROM users IGNORE INDEX FOR ORDER BY (idx_users_name)");

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        assertEquals(normalizeSql("SELECT * FROM users"), normalizeSql(result.sql().orElseThrow()));
        assertEquals("MYSQL_HINTS_DROPPED", result.warnings().getFirst().code());
    }

    @Test
    void droppedMySqlHintsCanBeRejectedByWarningPolicy() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .options(new TranspileOptions(false, true, true, true))
            .build();

        var result = transpiler.transpile("SELECT * FROM users USE INDEX (idx_users_name)");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("WARNINGS_NOT_ALLOWED", result.problems().getFirst().code());
        assertEquals("MYSQL_HINTS_DROPPED", result.warnings().getFirst().code());
    }

    @Test
    void mysqlOnDuplicateKeyUpdateIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile(
            "INSERT INTO users (id, name) VALUES (1, 'alice') ON DUPLICATE KEY UPDATE name = 'alice2'"
        );

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("UNSUPPORTED_ON_DUPLICATE_KEY_UPDATE", result.problems().getFirst().code());
    }

    @Test
    void mysqlInsertIgnoreIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("INSERT IGNORE INTO users (id) VALUES (1)");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("UNSUPPORTED_INSERT_IGNORE", result.problems().getFirst().code());
    }

    @Test
    void mysqlReplaceIntoIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("REPLACE INTO users (id) VALUES (1)");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("UNSUPPORTED_REPLACE_INTO", result.problems().getFirst().code());
    }

    @Test
    void mysqlJsonFunctionFamilyIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("SELECT JSON_EXTRACT(payload, '$.user.id') FROM users");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("UNSUPPORTED_MYSQL_JSON_FUNCTION", result.problems().getFirst().code());
    }

    @Test
    void mysqlJsonObjectFunctionFamilyIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("SELECT JSON_OBJECT('id', user_id, 'name', user_name) FROM users");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("UNSUPPORTED_MYSQL_JSON_FUNCTION", result.problems().getFirst().code());
    }

    @Test
    void mysqlJsonArrayFunctionFamilyIsRejectedBeforeRendering() {
        var transpiler = SqlTranspiler.builder()
            .sourceDialect(SqlDialectId.MYSQL)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("SELECT JSON_ARRAY(user_id, user_name) FROM users");

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("UNSUPPORTED_MYSQL_JSON_FUNCTION", result.problems().getFirst().code());
    }

    private static TranspileRule approximateRule() {
        return new TranspileRule() {
            @Override
            public String id() {
                return "approx-rule";
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
                return TranspileRuleResult.rewritten(statement, RewriteFidelity.APPROXIMATE, "Approx");
            }
        };
    }

    private static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}

