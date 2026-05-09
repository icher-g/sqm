package io.sqm.validate.schema.rule;

import io.sqm.core.HierarchicalQueryClause;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.Objects;

/**
 * Validates dialect support for hierarchical query clauses.
 */
public final class HierarchicalQueryFeatureValidationRule implements SchemaValidationRule<HierarchicalQueryClause> {
    private final String dialectName;
    private final SqlDialectVersion version;
    private final DialectCapabilities capabilities;

    /**
     * Creates a hierarchical query feature validation rule.
     *
     * @param dialectName dialect name used in validation messages
     * @param version dialect version used in validation messages
     * @param capabilities dialect capabilities
     */
    public HierarchicalQueryFeatureValidationRule(
        String dialectName,
        SqlDialectVersion version,
        DialectCapabilities capabilities
    ) {
        this.dialectName = Objects.requireNonNull(dialectName, "dialectName");
        this.version = Objects.requireNonNull(version, "version");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    /**
     * Returns the node type this rule validates.
     *
     * @return hierarchical query clause class
     */
    @Override
    public Class<HierarchicalQueryClause> nodeType() {
        return HierarchicalQueryClause.class;
    }

    /**
     * Validates dialect support for the provided hierarchical query clause.
     *
     * @param node hierarchical query clause
     * @param context validation context
     */
    @Override
    public void validate(HierarchicalQueryClause node, SchemaValidationContext context) {
        if (!capabilities.supports(SqlFeature.HIERARCHICAL_QUERY)) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                dialectName + " " + version + " does not support " + SqlFeature.HIERARCHICAL_QUERY.description(),
                node,
                "query.hierarchical"
            );
        }
    }
}
