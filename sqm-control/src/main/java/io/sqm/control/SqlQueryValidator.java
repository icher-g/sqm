package io.sqm.control;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.Query;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.postgresql.PostgresValidationDialect;
import io.sqm.validate.schema.SchemaQueryValidator;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.dialect.SchemaValidationDialect;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Validates parsed query models against schema and dialect policies.
 */
public interface SqlQueryValidator {

    /**
     * Creates the default dialect-aware validator for the provided schema.
     *
     * @param schema catalog schema
     * @return query validator
     */
    static SqlQueryValidator standard(CatalogSchema schema) {
        var settings = SchemaValidationSettings.defaults();
        return standard(schema, settings);
    }

    /**
     * Creates the default dialect-aware validator for the provided schema and settings.
     *
     * @param schema   catalog schema
     * @param settings validation settings
     * @return query validator
     */
    static SqlQueryValidator standard(CatalogSchema schema, SchemaValidationSettings settings) {
        return dialectAware(schema, Map.of(
            "ansi", () -> settings,
            "postgresql", () -> mergeDialectSettings(settings, PostgresValidationDialect.of()),
            "postgres", () -> mergeDialectSettings(settings, PostgresValidationDialect.of())
        ));
    }

    /**
     * Creates a dialect-aware validator with custom dialect settings mappings.
     *
     * @param schema         catalog schema
     * @param specsByDialect mapping of normalized dialect names to settings suppliers
     * @return query validator
     */
    static SqlQueryValidator dialectAware(CatalogSchema schema, Map<String, Supplier<SchemaValidationSettings>> specsByDialect) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(specsByDialect, "specsByDialect must not be null");
        var mappings = Map.copyOf(specsByDialect);

        return (sql, context) -> {
            Objects.requireNonNull(sql, "sql must not be null");
            Objects.requireNonNull(context, "context must not be null");

            var specsFactory = mappings.get(context.dialect().toLowerCase(Locale.ROOT));
            if (specsFactory == null) {
                throw new IllegalArgumentException("Unsupported dialect: " + context.dialect());
            }

            var baseSettings = specsFactory.get();
            var effectiveSettings = withPrincipal(baseSettings, context.principal());
            var validator = SchemaQueryValidator.of(schema, effectiveSettings);
            var result = validator.validate(sql);
            if (result.ok()) {
                return QueryValidateResult.ok();
            }

            var first = result.problems().getFirst();
            return QueryValidateResult.failure(mapReason(first.code()), first.message());
        };
    }

    private static SchemaValidationSettings mergeDialectSettings(SchemaValidationSettings base, SchemaValidationDialect dialect) {
        return SchemaValidationSettings.builder()
            .functionCatalog(dialect.functionCatalog())
            .accessPolicy(base.accessPolicy())
            .principal(base.principal())
            .limits(base.limits())
            .addRules(dialect.additionalRules())
            .addRules(base.additionalRules())
            .build();
    }

    private static SchemaValidationSettings withPrincipal(SchemaValidationSettings settings, String principal) {
        var effectivePrincipal = (principal != null && !principal.isBlank())
            ? principal
            : settings.principal();
        if (Objects.equals(effectivePrincipal, settings.principal())) {
            return settings;
        }
        return SchemaValidationSettings.builder()
            .functionCatalog(settings.functionCatalog())
            .accessPolicy(settings.accessPolicy())
            .principal(effectivePrincipal)
            .limits(settings.limits())
            .addRules(settings.additionalRules())
            .build();
    }

    private static ReasonCode mapReason(ValidationProblem.Code code) {
        return switch (code) {
            case DDL_NOT_ALLOWED -> ReasonCode.DENY_DDL;
            case DML_NOT_ALLOWED -> ReasonCode.DENY_DML;
            case POLICY_TABLE_DENIED -> ReasonCode.DENY_TABLE;
            case POLICY_COLUMN_DENIED -> ReasonCode.DENY_COLUMN;
            case POLICY_FUNCTION_NOT_ALLOWED -> ReasonCode.DENY_FUNCTION;
            case POLICY_MAX_JOINS_EXCEEDED -> ReasonCode.DENY_MAX_JOINS;
            case POLICY_MAX_SELECT_COLUMNS_EXCEEDED -> ReasonCode.DENY_MAX_SELECT_COLUMNS;
            case DIALECT_FEATURE_UNSUPPORTED, DIALECT_CLAUSE_INVALID -> ReasonCode.DENY_UNSUPPORTED_DIALECT_FEATURE;
            default -> ReasonCode.DENY_VALIDATION;
        };
    }

    /**
    * Validates query model for the provided execution context.
    *
    * @param query   query model
    * @param context execution context
    * @return validation result
    */
    QueryValidateResult validate(Query query, ExecutionContext context);
}
