package io.sqm.control;

import io.sqm.catalog.access.DefaultCatalogAccessPolicy;
import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.TenantRequirementMode;
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

    @Test
    void standard_uses_execution_context_tenant_for_tenant_aware_policy() {
        var policy = DefaultCatalogAccessPolicy.builder()
            .denyTableForTenant("tenant_a", "users")
            .build();
        var settings = SchemaValidationSettings.builder()
            .accessPolicy(policy)
            .build();
        var validator = SqlQueryValidator.standard(SCHEMA, settings);
        var query = select(col("id")).from(tbl("users")).build();

        var denied = validator.validate(
            query,
            ExecutionContext.of("postgresql", "alice", "tenant_a", ExecutionMode.ANALYZE)
        );
        assertEquals(ReasonCode.DENY_TABLE, denied.code());
        assertTrue(denied.isFailed());

        var allowed = validator.validate(
            query,
            ExecutionContext.of("postgresql", "alice", "tenant_b", ExecutionMode.ANALYZE)
        );
        assertEquals(ReasonCode.NONE, allowed.code());
    }

    @Test
    void standard_denies_when_tenant_is_required_and_missing() {
        var settings = SchemaValidationSettings.builder()
            .tenantRequirementMode(TenantRequirementMode.REQUIRED)
            .build();
        var validator = SqlQueryValidator.standard(SCHEMA, settings);
        var query = select(lit(1)).build();

        var denied = validator.validate(
            query,
            ExecutionContext.of("postgresql", "alice", null, ExecutionMode.ANALYZE)
        );

        assertEquals(ReasonCode.DENY_TENANT_REQUIRED, denied.code());
        assertTrue(denied.isFailed());
    }

    @Test
    void standard_allows_required_tenant_when_default_tenant_is_configured() {
        var settings = SchemaValidationSettings.builder()
            .tenantRequirementMode(TenantRequirementMode.REQUIRED)
            .tenant("tenant_a")
            .build();
        var validator = SqlQueryValidator.standard(SCHEMA, settings);
        var query = select(lit(1)).build();

        var allowed = validator.validate(
            query,
            ExecutionContext.of("postgresql", "alice", null, ExecutionMode.ANALYZE)
        );

        assertEquals(ReasonCode.NONE, allowed.code());
    }

    @Test
    void standard_uses_context_tenant_over_default_tenant_for_policy_resolution() {
        var policy = DefaultCatalogAccessPolicy.builder()
            .denyTableForTenant("tenant_a", "users")
            .build();
        var settings = SchemaValidationSettings.builder()
            .tenant("tenant_b")
            .accessPolicy(policy)
            .build();
        var validator = SqlQueryValidator.standard(SCHEMA, settings);
        var query = select(col("id")).from(tbl("users")).build();

        var denied = validator.validate(
            query,
            ExecutionContext.of("postgresql", "alice", "tenant_a", ExecutionMode.ANALYZE)
        );
        assertEquals(ReasonCode.DENY_TABLE, denied.code());

        var allowed = validator.validate(
            query,
            ExecutionContext.of("postgresql", "alice", null, ExecutionMode.ANALYZE)
        );
        assertEquals(ReasonCode.NONE, allowed.code());
    }
}
