package io.sqm.validate;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.SelectItem;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaStatementValidator;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaStatementValidatorDmlTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING),
            CatalogColumn.of("tenant_id", CatalogType.STRING)
        ),
        CatalogTable.of("public", "orders",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("user_id", CatalogType.LONG)
        )
    );

    @Test
    void validate_accepts_insert_with_existing_target_columns() {
        var validator = SchemaStatementValidator.of(SCHEMA);
        var statement = insert("users")
            .columns(id("id"), id("name"))
            .values(row(lit(1L), lit("alice")))
            .build();

        var result = validator.validate(statement);

        assertTrue(result.ok());
    }

    @Test
    void validate_reports_missing_insert_target_column() {
        var validator = SchemaStatementValidator.of(SCHEMA);
        var statement = insert("users")
            .columns(id("id"), id("missing_col"))
            .values(row(lit(1L), lit("alice")))
            .build();

        var result = validator.validate(statement);

        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(problem -> problem.code() == ValidationProblem.Code.COLUMN_NOT_FOUND
                && "insert.columns".equals(problem.clausePath())));
    }

    @Test
    void validate_reports_duplicate_insert_target_column() {
        var validator = SchemaStatementValidator.of(SCHEMA);
        var statement = insert("users")
            .columns(id("id"), id("id"))
            .values(row(lit(1L), lit(2L)))
            .build();

        var result = validator.validate(statement);

        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(problem -> problem.code() == ValidationProblem.Code.COLUMN_AMBIGUOUS
                && "insert.columns".equals(problem.clausePath())));
    }

    @Test
    void validate_reports_missing_update_assignment_target() {
        var validator = SchemaStatementValidator.of(SCHEMA);
        var statement = update(tbl("users").as("u"))
            .set(set("u", "missing_col", lit("alice")))
            .where(col("u", "id").eq(lit(1L)))
            .build();

        var result = validator.validate(statement);

        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(problem -> problem.code() == ValidationProblem.Code.COLUMN_NOT_FOUND
                && "dml.assignment".equals(problem.clausePath())));
    }

    @Test
    void validate_reports_unknown_update_assignment_alias() {
        var validator = SchemaStatementValidator.of(SCHEMA);
        var statement = update(tbl("users").as("u"))
            .set(set("missing", "name", lit("alice")))
            .where(col("u", "id").eq(lit(1L)))
            .build();

        var result = validator.validate(statement);

        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(problem -> problem.code() == ValidationProblem.Code.UNKNOWN_TABLE_ALIAS
                && "dml.assignment".equals(problem.clausePath())));
    }

    @Test
    void validate_accepts_delete_with_using_scope_references() {
        var validator = SchemaStatementValidator.of(SCHEMA);
        var statement = delete("users")
            .using(tbl("orders").as("o"))
            .where(col("o", "user_id").eq(col("users", "id")))
            .build();

        var result = validator.validate(statement);

        assertTrue(result.ok());
    }

    @Test
    void validate_accepts_insert_select_source_and_returning_scope() {
        var validator = SchemaStatementValidator.of(SCHEMA);
        var statement = insert(tbl("users").as("u"))
            .columns(id("id"), id("name"))
            .query(select(lit(1L), lit("alice")).build())
            .returning(SelectItem.expr(col("u", "id")))
            .build();

        var result = validator.validate(statement);

        assertTrue(result.ok());
    }
}

