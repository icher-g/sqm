package io.sqm.validate.postgresql;

import io.sqm.core.CteDef;
import io.sqm.core.MergeStatement;
import io.sqm.core.Query;
import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaStatementValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresValidationDialectTest {
    // Touch file to force test recompilation after Dsl.select(...) return-type refactor.
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    @Test
    void validate_reportsUnsupportedLateralInPostgres90() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(9, 0)));
        Query query = select(star()).from(tbl("users").lateral()).build();

        var result = validator.validate(query);

        assertTrue(hasUnsupportedFeature(result.problems(), "from.lateral"));
    }

    @Test
    void validate_reportsUnsupportedWithOrdinalityInPostgres93() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(9, 3)));
        Query query = select(star())
            .from(tbl(func("unnest", arg(array(lit(1L))))).as("u").withOrdinality())
            .build();

        var result = validator.validate(query);

        assertTrue(hasUnsupportedFeature(result.problems(), "from.function"));
    }

    @Test
    void validate_reportsUnsupportedCteMaterializationInPostgres90() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(9, 0)));
        Query query = with(
            cte(
                "u",
                select(col("id")).from(tbl("users")).build(),
                List.of("id"),
                CteDef.Materialization.MATERIALIZED
            )
        ).body(select(col("u", "id")).from(tbl("u").as("u")).build());

        var result = validator.validate(query);

        assertTrue(hasUnsupportedFeature(result.problems(), "with.cte"));
    }

    @Test
    void validate_reportsUnsupportedLockModeInPostgres90() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(9, 0)));
        Query query = select(star())
            .from(tbl("users").as("u"))
            .lockFor(keyShare(), List.of(), false, false)
            .build();

        var result = validator.validate(query);

        assertTrue(hasUnsupportedFeature(result.problems(), "select.lock"));
    }

    @Test
    void validate_reportsUnsupportedGroupingSetsInPostgres90() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(9, 0)));
        Query query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .groupBy(groupingSets(group("u", "id")))
            .build();

        var result = validator.validate(query);

        assertTrue(hasUnsupportedFeature(result.problems(), "select.group_by"));
    }

    @Test
    void validate_acceptsPostgres12Features() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = with(
            cte(
                "u",
                select(col("id")).from(tbl("users")).build(),
                List.of("id"),
                CteDef.Materialization.MATERIALIZED
            )
        ).body(
            select(star())
                .from(tbl("u").as("u").lateral())
                .lockFor(keyShare(), List.of(), false, false)
                .build()
        );

        var result = validator.validate(query);

        assertFalse(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED));
    }

    @Test
    void validate_reportsPostgresFunctionSignatureMismatch() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(func("generate_series", arg(lit("a")), arg(lit("b"))))
            .from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.FUNCTION_SIGNATURE_MISMATCH
                && "function.call".equals(p.clausePath())));
    }

    @Test
    void validate_usesPostgresFunctionReturnTypeForLimitInference() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(star())
            .from(tbl("users").as("u"))
            .limit(func("to_jsonb", arg(col("u", "id"))))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.LIMIT_OFFSET_INVALID));
    }

    @Test
    void validate_reportsDistinctOnLeftmostOrderByMismatch() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(col("u", "name"), col("u", "id"))
            .from(tbl("users").as("u"))
            .distinct(distinctOn(col("u", "name")))
            .orderBy(order(col("u", "id")))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DISTINCT_ON_ORDER_BY_MISMATCH
                && "distinct".equals(p.clausePath())));
    }

    @Test
    void validate_reportsDistinctOnOrderByOrdinalOutOfRange() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(col("u", "name"), col("u", "id"))
            .from(tbl("users").as("u"))
            .distinct(distinctOn(col("u", "name")))
            .orderBy(order(3))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DISTINCT_ON_ORDER_BY_MISMATCH
                && "distinct".equals(p.clausePath())));
    }

    @Test
    void validate_reportsDistinctOnOrderByOrdinalPointingToNonExpressionSelectItem() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(star(), col("u", "id"))
            .from(tbl("users").as("u"))
            .distinct(distinctOn(col("u", "id")))
            .orderBy(order(1))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DISTINCT_ON_ORDER_BY_MISMATCH
                && "distinct".equals(p.clausePath())));
    }

    @Test
    void validate_acceptsDistinctOnWithMatchingLeftmostOrderBy() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(col("u", "name"), col("u", "id"))
            .from(tbl("users").as("u"))
            .distinct(distinctOn(col("u", "name")))
            .orderBy(order(col("u", "name")), order(col("u", "id")))
            .build();

        var result = validator.validate(query);

        assertFalse(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DISTINCT_ON_ORDER_BY_MISMATCH));
    }

    @Test
    void validate_acceptsDistinctOnWithMatchingOrderByOrdinal() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(col("u", "name"), col("u", "id"))
            .from(tbl("users").as("u"))
            .distinct(distinctOn(col("u", "name")))
            .orderBy(order(1), order(2))
            .build();

        var result = validator.validate(query);

        assertFalse(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DISTINCT_ON_ORDER_BY_MISMATCH));
    }

    @Test
    void validate_reportsInvalidOrderByUsingWithDirection() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .orderBy(order(col("u", "id")).using(">").asc())
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.order".equals(p.clausePath())));
    }

    @Test
    void validate_reportsInvalidLateralBaseTableInSupportedVersion() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(star()).from(tbl("users").lateral()).build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "from.lateral".equals(p.clausePath())));
    }

    @Test
    void validate_reportsInvalidLockingWithDistinct() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .distinct(distinctOn(col("u", "id")))
            .lockFor(update(), List.of(), false, false).build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.lock".equals(p.clausePath())));
    }

    @Test
    void validate_reportsInvalidLockingWithGroupBy() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .groupBy(group("u", "id"))
            .lockFor(update(), List.of(), false, false)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.lock".equals(p.clausePath())));
    }

    @Test
    void validate_reportsInvalidLockingWithWindowClause() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .window(window("w", partition(col("u", "id"))))
            .lockFor(update(), List.of(), false, false)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.lock".equals(p.clausePath())));
    }

    @Test
    void validate_reportsInvalidLockingWithoutFrom() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(lit(1L))
            .lockFor(update(), List.of(), false, false)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.lock".equals(p.clausePath())));
    }

    @Test
    void validate_reportsInvalidOrderByUsingBlankOperator() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .orderBy(order(col("u", "id")).using(" "))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.order".equals(p.clausePath())));
    }

    @Test
    void validate_reportsInvalidNestedLateralWrappers() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(star()).from(tbl("users").lateral().lateral()).build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "from.lateral".equals(p.clausePath())));
    }

    @Test
    void validate_reportsInvalidDuplicateLockTargets() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(star())
            .from(tbl("users").as("u"))
            .lockFor(update(), ofTables("u", "u"), false, false)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.lock".equals(p.clausePath())));
    }

    @Test
    void validate_reportsInvalidLockTargetOnNullableSideOfLeftJoin() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(left(tbl("users").as("o")).on(col("o", "id").eq(col("u", "id"))))
            .lockFor(update(), ofTables("o"), false, false)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.lock".equals(p.clausePath())));
    }

    @Test
    void validate_reportsInvalidLockTargetOnNullableSideOfRightJoin() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(right(tbl("users").as("o")).on(col("o", "id").eq(col("u", "id"))))
            .lockFor(update(), ofTables("u"), false, false)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.lock".equals(p.clausePath())));
    }

    @Test
    void validate_reportsInvalidLockTargetOnNullableSideOfFullJoin() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(12, 0)));
        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(full(tbl("users").as("o")).on(col("o", "id").eq(col("u", "id"))))
            .lockFor(update(), ofTables("u"), false, false)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.lock".equals(p.clausePath())));
    }

    @Test
    void validate_reportsPostgresStatementHintsAsUnsupported() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(18, 0)));
        Query query = select(col("id"))
            .from(tbl("users"))
            .hint("MAX_EXECUTION_TIME", 1000)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "select.hint".equals(p.clausePath())));
    }

    @Test
    void validate_reportsPostgresStatementHintsAsUnsupportedAcrossDmlStatements() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(18, 0)));
        var insertStatement = insert("users")
            .columns(id("id"))
            .values(rows(row(lit(1L))))
            .hint("MAX_EXECUTION_TIME", 1000)
            .build();
        var updateStatement = update("users")
            .set(id("name"), lit("alice"))
            .hint("MAX_EXECUTION_TIME", 1000)
            .build();
        var deleteStatement = delete("users")
            .hint("MAX_EXECUTION_TIME", 1000)
            .build();
        var mergeStatement = merge("users")
            .source(tbl("users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .hint("MAX_EXECUTION_TIME", 1000)
            .build();

        var insertResult = validator.validate(insertStatement);
        var updateResult = validator.validate(updateStatement);
        var deleteResult = validator.validate(deleteStatement);
        var mergeResult = validator.validate(mergeStatement);

        assertTrue(insertResult.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "insert.hint".equals(p.clausePath())));
        assertTrue(updateResult.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "update.hint".equals(p.clausePath())));
        assertTrue(deleteResult.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "delete.hint".equals(p.clausePath())));
        assertTrue(mergeResult.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "merge.hint".equals(p.clausePath())));
    }

    @Test
    void validate_reportsPostgresTableHintsAsUnsupported() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(18, 0)));
        Query query = select(col("id"))
            .from(tbl("users").hint("NOLOCK"))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "table.hint".equals(p.clausePath())));
    }

    @Test
    void dialect_ofDefaultsToLatestSupportedVersion() {
        var dialect = PostgresValidationDialect.of();

        assertEquals(SqlDialectVersion.of(18, 0), dialect.version());
        assertEquals("postgresql", dialect.name());
        assertTrue(dialect.capabilities().supports(SqlFeature.CTE_MATERIALIZATION));
    }

    @Test
    void dialect_additionalRulesAndFunctionCatalogAreWired() {
        var dialect = PostgresValidationDialect.of(SqlDialectVersion.of(9, 0));

        var catalog = dialect.functionCatalog();
        var rules = dialect.additionalRules();

        assertNotNull(catalog);
        assertTrue(catalog.resolve("to_json").isPresent());
        assertFalse(catalog.resolve("to_jsonb").isPresent());
        assertEquals(7, rules.size());
        assertTrue(rules.stream().anyMatch(r -> r.getClass().getSimpleName().equals("PostgresSelectFeatureValidationRule")));
        assertTrue(rules.stream().anyMatch(r -> r.getClass().getSimpleName().equals("PostgresSelectClauseConsistencyRule")));
        assertTrue(rules.stream().anyMatch(r -> r.getClass().getSimpleName().equals("PostgresDistinctOnValidationRule")));
        assertTrue(rules.stream().anyMatch(r -> r.getClass().getSimpleName().equals("PostgresCteFeatureValidationRule")));
        assertTrue(rules.stream().anyMatch(r -> r.getClass().getSimpleName().equals("PostgresMergeFeatureValidationRule")));
        assertTrue(rules.stream().anyMatch(r -> r.getClass().getSimpleName().equals("PostgresStatementHintValidationRule")));
        assertTrue(rules.stream().anyMatch(r -> r.getClass().getSimpleName().equals("PostgresTableHintValidationRule")));
    }

    @Test
    void validate_reportsMergeUnsupportedBeforePostgres15() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(14, 0)));
        MergeStatement statement = merge("users")
            .source(tbl("users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .build();

        var result = validator.validate(statement);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "merge".equals(p.clausePath())));
    }

    @Test
    void validate_acceptsMergeFromPostgres15() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(15, 0)));
        MergeStatement statement = merge("users")
            .source(tbl("users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete(col("s", "name").eq(lit("closed")))
            .whenMatchedDoNothing(col("s", "id").gt(lit(0L)))
            .whenNotMatchedDoNothing()
            .whenNotMatchedInsert(col("s", "name").isNotNull(), java.util.List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .build();

        var result = validator.validate(statement);

        assertFalse(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "merge".equals(p.clausePath())));
    }

    @Test
    void validate_reportsMergeBySourceAndReturningBeforePostgres18() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(15, 0)));
        MergeStatement statement = merge("users")
            .source(tbl("users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenNotMatchedBySourceUpdate(col("users", "name").eq(lit("stale")), java.util.List.of(set("name", col("s", "name"))))
            .whenMatchedDelete()
            .result(col("id").toSelectItem())
            .build();

        var result = validator.validate(statement);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "merge.clause".equals(p.clausePath())));
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "merge.result".equals(p.clausePath())));
    }

    @Test
    void validate_acceptsMergeBySourceAndReturningFromPostgres18() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(18, 0)));
        MergeStatement statement = merge("users")
            .source(tbl("users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenNotMatchedBySourceUpdate(col("users", "name").eq(lit("stale")), java.util.List.of(set("name", col("s", "name"))))
            .whenMatchedDelete()
            .result(col("id").toSelectItem())
            .build();

        var result = validator.validate(statement);

        assertFalse(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && ("merge.clause".equals(p.clausePath()) || "merge.result".equals(p.clausePath()))));
    }

    @Test
    void validate_reportsMergeTopAsUnsupported() {
        var validator = SchemaStatementValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(15, 0)));
        MergeStatement statement = merge("users")
            .source(tbl("users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .top(top(5))
            .whenMatchedDelete()
            .build();

        var result = validator.validate(statement);

        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "merge.top".equals(p.clausePath())));
    }

    @Test
    void dialect_ofRejectsNullVersion() {
        assertThrows(NullPointerException.class, () -> PostgresValidationDialect.of(null));
    }

    private static boolean hasUnsupportedFeature(List<ValidationProblem> problems, String clausePath) {
        return problems.stream().anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
            && clausePath.equals(p.clausePath()));
    }
}

