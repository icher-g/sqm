package io.sqm.validate.postgresql;

import io.sqm.core.Node;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.postgresql.dialect.PostgresCapabilities;
import io.sqm.validate.schema.dialect.SchemaValidationDialect;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.postgresql.function.PostgresFunctionCatalog;
import io.sqm.validate.schema.rule.SchemaValidationRule;
import io.sqm.validate.postgresql.rule.PostgresSelectClauseConsistencyRule;
import io.sqm.validate.postgresql.rule.PostgresCteFeatureValidationRule;
import io.sqm.validate.postgresql.rule.PostgresDistinctOnValidationRule;
import io.sqm.validate.postgresql.rule.PostgresSelectFeatureValidationRule;

import java.util.List;
import java.util.Objects;

/**
 * PostgreSQL-specific schema validation dialect.
 *
 * <p>This dialect contributes PostgreSQL feature gating rules driven by
 * PostgreSQL version capabilities.</p>
 */
public final class PostgresValidationDialect implements SchemaValidationDialect {
    private final SqlDialectVersion version;
    private final DialectCapabilities capabilities;

    private PostgresValidationDialect(SqlDialectVersion version) {
        this.version = Objects.requireNonNull(version, "version");
        this.capabilities = PostgresCapabilities.of(version);
    }

    /**
     * Creates PostgreSQL validation dialect for the latest supported version.
     *
     * @return PostgreSQL validation dialect.
     */
    public static PostgresValidationDialect of() {
        return new PostgresValidationDialect(SqlDialectVersion.of(18, 0));
    }

    /**
     * Creates PostgreSQL validation dialect for a specific version.
     *
     * @param version PostgreSQL version.
     * @return PostgreSQL validation dialect.
     */
    public static PostgresValidationDialect of(SqlDialectVersion version) {
        return new PostgresValidationDialect(version);
    }

    /**
     * Returns PostgreSQL version used for feature checks.
     *
     * @return configured PostgreSQL version.
     */
    public SqlDialectVersion version() {
        return version;
    }

    /**
     * Returns dialect capabilities used by feature-gating rules.
     *
     * @return PostgreSQL capabilities.
     */
    public DialectCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public String name() {
        return "postgresql";
    }

    @Override
    public FunctionCatalog functionCatalog() {
        return PostgresFunctionCatalog.of(version);
    }

    @Override
    public List<SchemaValidationRule<? extends Node>> additionalRules() {
        return List.of(
            new PostgresSelectFeatureValidationRule(capabilities, version),
            new PostgresSelectClauseConsistencyRule(capabilities),
            new PostgresDistinctOnValidationRule(),
            new PostgresCteFeatureValidationRule(capabilities, version)
        );
    }
}
