package io.sqm.validate.sqlserver;

import io.sqm.core.Node;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.sqlserver.dialect.SqlServerCapabilities;
import io.sqm.validate.schema.dialect.SchemaValidationDialect;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.rule.SchemaValidationRule;
import io.sqm.validate.sqlserver.function.SqlServerFunctionCatalog;
import io.sqm.validate.sqlserver.rule.SqlServerDeleteStatementValidationRule;
import io.sqm.validate.sqlserver.rule.SqlServerExpressionFeatureValidationRule;
import io.sqm.validate.sqlserver.rule.SqlServerFunctionOrderByValidationRule;
import io.sqm.validate.sqlserver.rule.SqlServerAnyAllPredicateValidationRule;
import io.sqm.validate.sqlserver.rule.SqlServerInsertStatementValidationRule;
import io.sqm.validate.sqlserver.rule.SqlServerMergeStatementValidationRule;
import io.sqm.validate.sqlserver.rule.SqlServerSelectValidationRule;
import io.sqm.validate.sqlserver.rule.SqlServerUpdateStatementValidationRule;

import java.util.List;
import java.util.Objects;

/**
 * SQL Server-specific schema validation dialect.
 *
 * <p>This dialect contributes SQL Server baseline query and DML validation
 * rules that are not enforced by the shared schema validator.</p>
 */
public final class SqlServerValidationDialect implements SchemaValidationDialect {
    private final SqlDialectVersion version;
    private final DialectCapabilities capabilities;

    private SqlServerValidationDialect(SqlDialectVersion version) {
        this.version = Objects.requireNonNull(version, "version");
        this.capabilities = SqlServerCapabilities.of(version);
    }

    /**
     * Returns the shared SQL Server validation dialect instance.
     *
     * @return SQL Server validation dialect.
     */
    public static SqlServerValidationDialect of() {
        return new SqlServerValidationDialect(SqlDialectVersion.of(2019, 0));
    }

    /**
     * Creates SQL Server validation dialect for a specific version.
     *
     * @param version SQL Server version used for feature checks.
     * @return SQL Server validation dialect.
     */
    public static SqlServerValidationDialect of(SqlDialectVersion version) {
        return new SqlServerValidationDialect(version);
    }

    /**
     * Returns SQL Server version used for feature checks.
     *
     * @return configured SQL Server version.
     */
    public SqlDialectVersion version() {
        return version;
    }

    /**
     * Returns dialect capabilities used by SQL Server feature-gating rules.
     *
     * @return SQL Server capabilities.
     */
    public DialectCapabilities capabilities() {
        return capabilities;
    }

    /**
     * Returns the dialect name used by validation callers.
     *
     * @return dialect name.
     */
    @Override
    public String name() {
        return SqlDialectId.SQLSERVER.value();
    }

    /**
     * Returns SQL Server-specific function signatures used by validation.
     *
     * @return SQL Server function catalog.
     */
    @Override
    public FunctionCatalog functionCatalog() {
        return SqlServerFunctionCatalog.standard();
    }

    /**
     * Returns SQL Server-specific validation rules.
     *
     * @return immutable list of additional rules.
     */
    @Override
    public List<SchemaValidationRule<? extends Node>> additionalRules() {
        return List.of(
            new SqlServerExpressionFeatureValidationRule(capabilities, version),
            new SqlServerSelectValidationRule(),
            new SqlServerInsertStatementValidationRule(),
            new SqlServerUpdateStatementValidationRule(),
            new SqlServerDeleteStatementValidationRule(),
            new SqlServerMergeStatementValidationRule(),
            new SqlServerAnyAllPredicateValidationRule(),
            new SqlServerFunctionOrderByValidationRule()
        );
    }
}
