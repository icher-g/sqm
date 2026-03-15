package io.sqm.validate.sqlserver;

import io.sqm.core.Node;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.validate.schema.dialect.SchemaValidationDialect;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.rule.SchemaValidationRule;
import io.sqm.validate.sqlserver.function.SqlServerFunctionCatalog;
import io.sqm.validate.sqlserver.rule.SqlServerDeleteStatementValidationRule;
import io.sqm.validate.sqlserver.rule.SqlServerInsertStatementValidationRule;
import io.sqm.validate.sqlserver.rule.SqlServerSelectValidationRule;
import io.sqm.validate.sqlserver.rule.SqlServerUpdateStatementValidationRule;

import java.util.List;

/**
 * SQL Server-specific schema validation dialect.
 *
 * <p>This dialect contributes SQL Server baseline query and DML validation
 * rules that are not enforced by the shared schema validator.</p>
 */
public final class SqlServerValidationDialect implements SchemaValidationDialect {
    private static final SqlServerValidationDialect INSTANCE = new SqlServerValidationDialect();

    private SqlServerValidationDialect() {
    }

    /**
     * Returns the shared SQL Server validation dialect instance.
     *
     * @return SQL Server validation dialect.
     */
    public static SqlServerValidationDialect of() {
        return INSTANCE;
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
            new SqlServerSelectValidationRule(),
            new SqlServerInsertStatementValidationRule(),
            new SqlServerUpdateStatementValidationRule(),
            new SqlServerDeleteStatementValidationRule()
        );
    }
}
