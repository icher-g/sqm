package io.sqm.control;

import io.sqm.catalog.access.DefaultCatalogAccessPolicy;
import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.*;

class SqlQueryValidatorTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    @Test
    void standard_maps_validation_problems_to_reason_codes() {
        var settings = SchemaValidationSettings.builder()
            .limits(SchemaValidationLimits.builder().maxSelectColumns(1).build())
            .build();
        var validator = SqlQueryValidator.standard(SCHEMA, settings);
        var query = select(lit(1), lit(2)).build();

        var result = validator.validate(query, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(ReasonCode.DENY_MAX_SELECT_COLUMNS, result.code());
        assertTrue(result.isFailed());
    }

    @Test
    void standard_returns_ok_for_valid_query() {
        var validator = SqlQueryValidator.standard(SCHEMA);
        var query = select(lit(1)).build();

        var result = validator.validate(query, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(ReasonCode.NONE, result.code());
    }

    @Test
    void dialect_aware_validates_configuration_and_dialect_resolution() {
        assertThrows(NullPointerException.class, () -> SqlQueryValidator.dialectAware(null, Map.of()));
        assertThrows(NullPointerException.class, () -> SqlQueryValidator.dialectAware(SCHEMA, null));

        var validator = SqlQueryValidator.dialectAware(
            SCHEMA,
            Map.of("ansi", SchemaValidationSettings::defaults)
        );
        var query = select(lit(1)).build();

        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate(query, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE))
        );
    }

    @Test
    void standard_uses_execution_context_principal_for_principal_aware_policy() {
        var policy = DefaultCatalogAccessPolicy.builder()
            .denyTableForPrincipal("alice", "users")
            .build();
        var settings = SchemaValidationSettings.builder()
            .accessPolicy(policy)
            .build();
        var validator = SqlQueryValidator.standard(SCHEMA, settings);
        var query = select(col("id")).from(tbl("users")).build();

        var denied = validator.validate(
            query,
            ExecutionContext.of("postgresql", "alice", null, ExecutionMode.ANALYZE)
        );
        assertEquals(ReasonCode.DENY_TABLE, denied.code());
        assertTrue(denied.isFailed());

        var allowed = validator.validate(
            query,
            ExecutionContext.of("postgresql", "bob", null, ExecutionMode.ANALYZE)
        );
        assertEquals(ReasonCode.NONE, allowed.code());
    }
}
