package io.sqm.validate;

import io.sqm.catalog.model.CatalogType;
import io.sqm.core.Identifier;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaStatementValidator;
import io.sqm.validate.schema.ValidationCatalogSchemas;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests permissive validator catalog schemas.
 */
class ValidationCatalogSchemasTest {

    @Test
    void allowEverything_allows_unknown_query_tables_and_columns() {
        var validator = SchemaStatementValidator.of(ValidationCatalogSchemas.allowEverything());

        var result = validator.validate(
            select(col("m", "mystery_col"))
                .from(tbl("mystery_table").as("m"))
                .where(col("m", "other_col").eq(lit(1)))
                .build()
        );

        assertTrue(result.ok());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void allowEverything_still_reports_unknown_aliases() {
        var validator = SchemaStatementValidator.of(ValidationCatalogSchemas.allowEverything());

        var result = validator.validate(
            select(col("missing", "id"))
                .from(tbl("known_enough").as("k"))
                .build()
        );

        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(problem -> problem.code() == ValidationProblem.Code.UNKNOWN_TABLE_ALIAS));
    }

    @Test
    void allowEverything_exposes_unknown_type_for_physical_dml_columns() {
        var context = new SchemaValidationContext(ValidationCatalogSchemas.allowEverything());

        var column = context.resolvePhysicalTableColumn(tbl("anything"), Identifier.of("any_column")).orElseThrow();

        assertEquals(CatalogType.UNKNOWN, column.type());
    }

    @Test
    void allowEverything_allows_insert_target_columns() {
        var validator = SchemaStatementValidator.of(ValidationCatalogSchemas.allowEverything());

        var result = validator.validate(
            insert("anything")
                .columns(id("first_col"), id("second_col"))
                .values(row(lit(1), lit("x")))
                .build()
        );

        assertTrue(result.ok());
        assertTrue(result.problems().isEmpty());
    }
}
