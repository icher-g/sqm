package io.sqm.validate.oracle;

import io.sqm.core.Node;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.oracle.dialect.OracleCapabilities;
import io.sqm.validate.oracle.function.OracleFunctionCatalog;
import io.sqm.validate.oracle.rule.OracleDmlFeatureValidationRule;
import io.sqm.validate.oracle.rule.OracleMergeFeatureValidationRule;
import io.sqm.validate.schema.dialect.SchemaValidationDialect;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.rule.HierarchicalQueryFeatureValidationRule;
import io.sqm.validate.schema.rule.JsonTableFeatureValidationRule;
import io.sqm.validate.schema.rule.PivotFeatureValidationRule;
import io.sqm.validate.schema.rule.SequenceValueFeatureValidationRule;
import io.sqm.validate.schema.rule.SchemaValidationRule;
import io.sqm.validate.schema.rule.TableAccessFeatureValidationRule;

import java.util.List;
import java.util.Objects;

/**
 * Oracle-specific schema validation dialect.
 */
public final class OracleValidationDialect implements SchemaValidationDialect {
    private final SqlDialectVersion version;
    private final DialectCapabilities capabilities;

    private OracleValidationDialect(SqlDialectVersion version) {
        this.version = Objects.requireNonNull(version, "version");
        this.capabilities = OracleCapabilities.of(version);
    }

    /**
     * Creates an Oracle validation dialect for the latest supported version.
     *
     * @return Oracle validation dialect.
     */
    public static OracleValidationDialect of() {
        return new OracleValidationDialect(SqlDialectVersion.of(19, 0));
    }

    /**
     * Creates an Oracle validation dialect for a specific version.
     *
     * @param version Oracle version used for feature checks.
     * @return Oracle validation dialect.
     */
    public static OracleValidationDialect of(SqlDialectVersion version) {
        return new OracleValidationDialect(version);
    }

    /**
     * Returns Oracle version used for feature checks.
     *
     * @return configured Oracle version.
     */
    public SqlDialectVersion version() {
        return version;
    }

    /**
     * Returns dialect capabilities used by Oracle feature-gating rules.
     *
     * @return Oracle capabilities.
     */
    public DialectCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public String name() {
        return SqlDialectId.ORACLE.value();
    }

    /**
     * Returns Oracle-specific function signatures.
     *
     * @return Oracle function catalog.
     */
    @Override
    public FunctionCatalog functionCatalog() {
        return OracleFunctionCatalog.of(version);
    }

    @Override
    public List<SchemaValidationRule<? extends Node>> additionalRules() {
        return List.of(
            new OracleDmlFeatureValidationRule(capabilities, version),
            new OracleMergeFeatureValidationRule(capabilities, version),
            new SequenceValueFeatureValidationRule(name(), version, capabilities, true),
            new HierarchicalQueryFeatureValidationRule(name(), version, capabilities),
            new PivotFeatureValidationRule(name(), version, capabilities),
            new JsonTableFeatureValidationRule(name(), version, capabilities),
            new TableAccessFeatureValidationRule(name(), version, capabilities)
        );
    }
}
