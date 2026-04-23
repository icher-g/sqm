package io.sqm.validate.mysql;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.Identifier;
import io.sqm.core.SelectModifier;
import io.sqm.core.TableHint;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaStatementValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class MySqlValidationDialectTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        ),
        CatalogTable.of("public", "orders",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("user_id", CatalogType.LONG)
        )
    );

    @Test
    void validate_reportsUnsupportedAggregateInputOrderBy() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var query = select(func("array_agg", col("u", "name")).orderBy(col("u", "name")))
            .from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "function.orderBy".equals(problem.clausePath())
        ));
    }

    @Test
    void validate_reportsUseAndForceConflictInSameDefaultScope() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u").withHints(List.of(
                TableHint.of("USE_INDEX", Identifier.of("idx_users_name")),
                TableHint.of("FORCE_INDEX", Identifier.of("idx_users_id"))
            )))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "table.index_hint".equals(problem.clausePath())
                && problem.message().contains("JOIN")
        ));
    }

    @Test
    void validate_reportsUseAndForceConflictForOverlappingScopes() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u").withHints(List.of(
                TableHint.of("USE_INDEX", Identifier.of("idx_users_name")),
                TableHint.of("FORCE_INDEX_FOR_JOIN", Identifier.of("idx_users_id"))
            )))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && problem.message().contains("JOIN")
        ));
    }

    @Test
    void validate_allowsUseAndForceForDifferentExplicitScopes() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u").withHints(List.of(
                TableHint.of("USE_INDEX_FOR_ORDER_BY", Identifier.of("idx_users_name")),
                TableHint.of("FORCE_INDEX_FOR_JOIN", Identifier.of("idx_users_id"))
            )))
            .build();

        var result = validator.validate(query);

        assertFalse(result.problems().stream()
            .anyMatch(problem -> problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID));
    }

    @Test
    void dialect_exposesMysqlIndexHintRule() {
        var dialect = MySqlValidationDialect.of();
        var versionedDialect = MySqlValidationDialect.of(SqlDialectVersion.of(8, 0, 13));

        assertEquals("mysql", dialect.name());
        assertEquals(SqlDialectVersion.of(8, 0, 14), dialect.version());
        assertTrue(dialect.capabilities().supports(io.sqm.core.dialect.SqlFeature.LATERAL));
        assertFalse(versionedDialect.capabilities().supports(io.sqm.core.dialect.SqlFeature.LATERAL));
        assertFalse(dialect.additionalRules().isEmpty());
    }

    @Test
    void validate_acceptsLateralDerivedTableFromMysql8014() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of(SqlDialectVersion.of(8, 0, 14)));
        var query = select(col("sq", "id"))
            .from(tbl(select(col("id")).from(tbl("users")).build()).as("sq").lateral())
            .build();

        var result = validator.validate(query);

        assertTrue(result.ok(), () -> result.problems().toString());
    }

    @Test
    void validate_reportsUnsupportedLateralBeforeMysql8014() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of(SqlDialectVersion.of(8, 0, 13)));
        var query = select(col("sq", "id"))
            .from(tbl(select(col("id")).from(tbl("users")).build()).as("sq").lateral())
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "from.lateral".equals(problem.clausePath())
                && problem.message().contains("LATERAL")
        ));
    }

    @Test
    void validate_reportsInvalidMysqlLateralShapeForBaseTables() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of(SqlDialectVersion.of(8, 0, 14)));
        var query = select(col("u", "id"))
            .from(tbl("users").as("u").lateral())
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "from.lateral".equals(problem.clausePath())
                && problem.message().contains("derived tables")
        ));
    }

    @Test
    void validate_reportsMissingAliasForMysqlLateralDerivedTables() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of(SqlDialectVersion.of(8, 0, 14)));
        var query = select(col("id"))
            .from(tbl(select(col("id")).from(tbl("users")).build()).lateral())
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "from.lateral".equals(problem.clausePath())
                && problem.message().contains("alias")
        ));
    }

    @Test
    void validate_reportsUnsupportedCalcFoundRowsBeforeMysql80() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of(SqlDialectVersion.of(5, 7)));
        var query = select(col("id"))
            .from(tbl("users"))
            .selectModifier(SelectModifier.CALC_FOUND_ROWS)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "select.modifier".equals(problem.clausePath())
                && problem.message().contains("SQL_CALC_FOUND_ROWS")
        ));
    }

    @Test
    void validate_skipsNestedLateralTraversalAndStillValidatesNestedQueries() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of(SqlDialectVersion.of(8, 0, 13)));
        var scalarQuery = select(
            io.sqm.core.Expression.subquery(
                select(col("sq", "id"))
                    .from(tbl(select(col("id")).from(tbl("users")).build()).as("sq").lateral())
                    .build()
            )
        )
            .from(tbl("users").as("u"))
            .build();
        var existsQuery = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .where(exists(
                select(col("sq", "id"))
                    .from(tbl(select(col("id")).from(tbl("users")).build()).as("sq").lateral())
                    .build()
            ))
            .build();
        var anyQuery = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .where(col("u", "id").any(io.sqm.core.ComparisonOperator.EQ,
                select(col("sq", "id"))
                    .from(tbl(select(col("id")).from(tbl("users")).build()).as("sq").lateral())
                    .build()))
            .build();

        var scalarResult = validator.validate(scalarQuery);
        var existsResult = validator.validate(existsQuery);
        var anyResult = validator.validate(anyQuery);

        assertEquals(1, scalarResult.problems().stream()
            .filter(problem -> "from.lateral".equals(problem.clausePath()))
            .count());
        assertEquals(1, existsResult.problems().stream()
            .filter(problem -> "from.lateral".equals(problem.clausePath()))
            .count());
        assertEquals(1, anyResult.problems().stream()
            .filter(problem -> "from.lateral".equals(problem.clausePath()))
            .count());
    }

    @Test
    void validate_reportsFunctionTableAsUnsupportedInMysql() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var query = select(col("jt", "id"))
            .from(tbl(func("generate_series", lit(1), lit(2))).as("jt"))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "from.function_table".equals(problem.clausePath())
                && problem.message().contains("Set-returning function")
        ));
    }

    @Test
    void validate_reportsFunctionTableOrdinalityAsUnsupportedInMysql() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var query = select(col("jt", "id"))
            .from(tbl(func("generate_series", lit(1), lit(2))).withOrdinality().as("jt"))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "from.function_table".equals(problem.clausePath())
                && problem.message().contains("Set-returning function")
        ));
    }

    @Test
    void mysql_dialect_retains_base_dml_validation_rules() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var statement = update("users")
            .set(Identifier.of("missing_col"), io.sqm.dsl.Dsl.lit("alice"))
            .build();

        var result = validator.validate(statement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.COLUMN_NOT_FOUND
                && "dml.assignment".equals(problem.clausePath())
        ));
    }

    @Test
    void validate_acceptsFirstWaveMysqlStatementHints() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var selectQuery = select(col("id"))
            .from(tbl("users"))
            .hint("MAX_EXECUTION_TIME", 1000)
            .build();
        var updateStatement = update("users")
            .set(Identifier.of("name"), lit("alice"))
            .hint("BKA", "users")
            .build();
        var deleteStatement = delete("users")
            .hint("SET_VAR", "sort_buffer_size=16M")
            .build();
        var insertStatement = insert("users")
            .columns(Identifier.of("id"))
            .values(io.sqm.dsl.Dsl.rows(io.sqm.dsl.Dsl.row(lit(1L))))
            .hint("QB_NAME", io.sqm.dsl.Dsl.lit("main"))
            .build();

        assertTrue(validator.validate(selectQuery).ok());
        assertTrue(validator.validate(updateStatement).ok());
        assertTrue(validator.validate(deleteStatement).ok());
        assertTrue(validator.validate(insertStatement).ok());
    }

    @Test
    void validate_reportsUnsupportedOptimizerHintsBeforeMysql80() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of(SqlDialectVersion.of(5, 7)));
        var query = select(col("id"))
            .from(tbl("users"))
            .hint("MAX_EXECUTION_TIME", 1000)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "select.hint".equals(problem.clausePath())
                && problem.message().contains("optimizer hint comment")
        ));
    }

    @Test
    void validate_reportsUnsupportedInsertIgnoreBeforeMysql80() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of(SqlDialectVersion.of(5, 7)));
        var statement = insert("users")
            .ignore()
            .columns(Identifier.of("id"))
            .values(rows(row(lit(1L))))
            .build();

        var result = validator.validate(statement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "insert.mode".equals(problem.clausePath())
                && problem.message().contains("INSERT IGNORE")
        ));
    }

    @Test
    void validate_reportsUnsupportedReplaceIntoBeforeMysql80() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of(SqlDialectVersion.of(5, 7)));
        var statement = insert("users")
            .replace()
            .columns(Identifier.of("id"))
            .values(rows(row(lit(1L))))
            .build();

        var result = validator.validate(statement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "insert.mode".equals(problem.clausePath())
                && problem.message().contains("REPLACE INTO")
        ));
    }

    @Test
    void validate_reportsUnsupportedOnDuplicateKeyUpdateBeforeMysql80() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of(SqlDialectVersion.of(5, 7)));
        var statement = insert("users")
            .columns(Identifier.of("id"), Identifier.of("name"))
            .values(rows(row(lit(1L), lit("alice"))))
            .onConflictDoUpdate(set("name", lit("updated")))
            .build();

        var result = validator.validate(statement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "insert.conflict".equals(problem.clausePath())
                && problem.message().contains("ON DUPLICATE KEY UPDATE")
        ));
    }

    @Test
    void validate_reportsUnsupportedInsertReturningInLatestMysqlSlice() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var statement = insert("users")
            .columns(Identifier.of("id"))
            .values(rows(row(lit(1L))))
            .result(col("id").toSelectItem())
            .build();

        var result = validator.validate(statement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "insert.result".equals(problem.clausePath())
                && problem.message().contains("DML result clause")
        ));
    }

    @Test
    void validate_reportsUnsupportedUpdateJoinBeforeMysql80() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of(SqlDialectVersion.of(5, 7)));
        var statement = update("users")
            .join(inner(tbl("orders").as("o")).on(col("o", "user_id").eq(col("users", "id"))))
            .set(Identifier.of("name"), lit("alice"))
            .build();

        var result = validator.validate(statement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "update.join".equals(problem.clausePath())
                && problem.message().contains("UPDATE ... JOIN")
        ));
    }

    @Test
    void validate_reportsUnsupportedDeleteUsingJoinBeforeMysql80() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of(SqlDialectVersion.of(5, 7)));
        var statement = delete("users")
            .using(tbl("orders").as("o"))
            .join(inner(tbl("users").as("u")).on(col("u", "id").eq(col("o", "user_id"))))
            .build();

        var result = validator.validate(statement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "delete.using".equals(problem.clausePath())
                && problem.message().contains("DELETE ... USING ... JOIN")
        ));
    }

    @Test
    void validate_reportsUnsupportedMysqlStatementHintNames() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var query = select(col("id"))
            .from(tbl("users"))
            .hint("INDEX", "users", "idx_users_name")
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "select.hint".equals(problem.clausePath())
                && problem.message().contains("INDEX")
        ));
    }

    @Test
    void validate_reportsInvalidMysqlStatementHintArgShapes() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var query = select(col("id"))
            .from(tbl("users"))
            .hint("MAX_EXECUTION_TIME", "users")
            .hint("BKA", 1000)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.hint".equals(problem.clausePath())
                && problem.message().contains("MAX_EXECUTION_TIME")
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.hint".equals(problem.clausePath())
                && problem.message().contains("BKA")
        ));
    }

    @Test
    void validate_reportsInvalidMysqlStatementHintShapesForAdditionalFamilies() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var query = select(col("id"))
            .from(tbl("users"))
            .hint("NO_RANGE_OPTIMIZATION", "users", "extra")
            .hint("SET_VAR", lit(1))
            .hint("QB_NAME", lit(1))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.hint".equals(problem.clausePath())
                && problem.message().contains("NO_RANGE_OPTIMIZATION")
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.hint".equals(problem.clausePath())
                && problem.message().contains("SET_VAR")
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.hint".equals(problem.clausePath())
                && problem.message().contains("QB_NAME")
        ));
    }

    @Test
    void validate_acceptsMysqlStatementHintsOnMergeAndQualifiedNameTargets() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var statement = merge("users")
            .source(tbl("users").as("src"))
            .on(col("users", "id").eq(col("src", "id")))
            .whenMatchedDelete()
            .hint("NO_RANGE_OPTIMIZATION", io.sqm.core.QualifiedName.of("public", "users"))
            .build();

        var result = validator.validate(statement);

        assertTrue(result.ok());
    }

    @Test
    void validate_reportsUnsupportedMysqlTableHintFamiliesAndArgShapes() {
        var validator = SchemaStatementValidator.of(SCHEMA, MySqlValidationDialect.of());
        var query = select(col("id"))
            .from(tbl("users")
                .hint("NOLOCK")
                .hint("USE_INDEX")
                .hint("FORCE_INDEX", 1000))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "table.hint".equals(problem.clausePath())
                && problem.message().contains("NOLOCK")
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "table.hint".equals(problem.clausePath())
                && problem.message().contains("at least one index identifier")
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "table.hint".equals(problem.clausePath())
                && problem.message().contains("identifier arguments")
        ));
    }
}

