package io.sqm.validate.mysql;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.Identifier;
import io.sqm.core.TableHint;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaStatementValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlValidationDialectTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

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

        assertEquals("mysql", dialect.name());
        assertFalse(dialect.additionalRules().isEmpty());
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

