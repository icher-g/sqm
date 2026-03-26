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
}

