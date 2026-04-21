package io.sqm.control;

import io.sqm.catalog.access.DefaultCatalogAccessPolicy;
import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.decision.ReasonCode;
import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.control.pipeline.SqlStatementValidator;
import io.sqm.core.Identifier;
import io.sqm.core.StatementSequence;
import io.sqm.core.UpdateStatement;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.TenantRequirementMode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlStatementValidatorTest {
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
        var validator = SqlStatementValidator.standard(SCHEMA, settings);
        var query = select(lit(1), lit(2)).build();

        var result = validator.validate(query, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(ReasonCode.DENY_MAX_SELECT_COLUMNS, result.code());
        assertTrue(result.isFailed());
    }

    @Test
    void standard_returns_ok_for_valid_query() {
        var validator = SqlStatementValidator.standard(SCHEMA);
        var query = select(lit(1)).build();

        var result = validator.validate(query, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(ReasonCode.NONE, result.code());
    }

    @Test
    void standard_supports_mysql_dialect_validation() {
        var validator = SqlStatementValidator.standard(SCHEMA);
        var statement = update("users")
            .set(Identifier.of("name"), lit("alice"))
            .where(col("id").eq(lit(1)))
            .build();

        var result = validator.validate(statement, ExecutionContext.of("mysql", ExecutionMode.ANALYZE));

        assertEquals(ReasonCode.NONE, result.code());
    }

    @Test
    void standard_supports_sqlserver_dialect_validation() {
        var validator = SqlStatementValidator.standard(SCHEMA);
        var query = select(col("id")).from(tbl(Identifier.of("users", io.sqm.core.QuoteStyle.BRACKETS))).build();

        var result = validator.validate(query, ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE));

        assertEquals(ReasonCode.NONE, result.code());
    }

    @Test
    void standard_supports_sqlserver_advanced_merge_validation() {
        var validator = SqlStatementValidator.standard(SCHEMA);
        var statement = select(col("u", "id"))
            .from(tbl(Identifier.of("users", io.sqm.core.QuoteStyle.BRACKETS)).as(Identifier.of("u", io.sqm.core.QuoteStyle.BRACKETS)).withNoLock())
            .top(io.sqm.dsl.Dsl.topPercent(lit(10)))
            .orderBy(io.sqm.dsl.Dsl.order(col("u", "id")))
            .build();

        var result = validator.validate(statement, ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE));

        assertEquals(ReasonCode.NONE, result.code());
    }

    @Test
    void standard_reports_dml_assignment_validation_failures() {
        var validator = SqlStatementValidator.standard(SCHEMA);
        UpdateStatement statement = update("users")
            .set(Identifier.of("missing_col"), lit("alice"))
            .build();

        var result = validator.validate(statement, ExecutionContext.of("ansi", ExecutionMode.ANALYZE));

        assertEquals(ReasonCode.DENY_VALIDATION, result.code());
        assertTrue(result.isFailed());
    }

    @Test
    void dialect_aware_validates_configuration_and_dialect_resolution() {
        assertThrows(NullPointerException.class, () -> SqlStatementValidator.dialectAware(null, Map.of()));
        assertThrows(NullPointerException.class, () -> SqlStatementValidator.dialectAware(SCHEMA, null));

        var validator = SqlStatementValidator.dialectAware(
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
        var validator = SqlStatementValidator.standard(SCHEMA, settings);
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
        var validator = SqlStatementValidator.standard(SCHEMA, settings);
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
        var validator = SqlStatementValidator.standard(SCHEMA, settings);
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
        var validator = SqlStatementValidator.standard(SCHEMA, settings);
        var query = select(lit(1)).build();

        var allowed = validator.validate(
            query,
            ExecutionContext.of("postgresql", "alice", null, ExecutionMode.ANALYZE)
        );

        assertEquals(ReasonCode.NONE, allowed.code());
    }

    @Test
    void standard_allows_required_tenant_when_execution_context_provides_tenant() {
        var settings = SchemaValidationSettings.builder()
            .tenantRequirementMode(TenantRequirementMode.REQUIRED)
            .build();
        var validator = SqlStatementValidator.standard(SCHEMA, settings);
        var query = select(lit(1)).build();

        var allowed = validator.validate(
            query,
            ExecutionContext.of("postgresql", "alice", "tenant_a", ExecutionMode.ANALYZE)
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
        var validator = SqlStatementValidator.standard(SCHEMA, settings);
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

    @Test
    void validates_statement_sequences_and_reports_statement_index() {
        var policy = DefaultCatalogAccessPolicy.builder()
            .denyTable("orders")
            .build();
        var settings = SchemaValidationSettings.builder()
            .accessPolicy(policy)
            .build();
        var validator = SqlStatementValidator.standard(CatalogSchema.of(
            CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG)),
            CatalogTable.of("public", "orders", CatalogColumn.of("id", CatalogType.LONG))
        ), settings);
        var sequence = StatementSequence.of(
            select(col("id")).from(tbl("users")).build(),
            select(col("id")).from(tbl("orders")).build()
        );

        var result = validator.validate(sequence, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(ReasonCode.DENY_TABLE, result.code());
        assertTrue(result.message().startsWith("Statement 2:"));
    }

    @Test
    void validates_statement_sequences_when_all_statements_are_valid() {
        var validator = SqlStatementValidator.standard(SCHEMA);
        var sequence = StatementSequence.of(
            select(lit(1)).build(),
            select(lit(2)).build()
        );

        var result = validator.validate(sequence, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(ReasonCode.NONE, result.code());
    }

    @Test
    void rejects_unsupported_node_for_sequence_entrypoint() {
        var validator = SqlStatementValidator.standard(SCHEMA);

        var result = validator.validate(lit(1), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(ReasonCode.DENY_PIPELINE_ERROR, result.code());
        assertTrue(result.isFailed());
    }
}
