package io.sqm.validate.schema.rule;

import io.sqm.core.LockMode;
import io.sqm.core.LockingClause;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.Objects;

/**
 * Validates dialect support for SELECT locking clause features.
 */
public final class LockingClauseFeatureValidationRule implements SchemaValidationRule<LockingClause> {
    private final String dialectName;
    private final SqlDialectVersion version;
    private final DialectCapabilities capabilities;

    /**
     * Creates a locking-clause feature validation rule.
     *
     * @param dialectName dialect name used in validation messages
     * @param version dialect version used in validation messages
     * @param capabilities dialect capabilities
     */
    public LockingClauseFeatureValidationRule(String dialectName, SqlDialectVersion version, DialectCapabilities capabilities) {
        this.dialectName = Objects.requireNonNull(dialectName, "dialectName");
        this.version = Objects.requireNonNull(version, "version");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    @Override
    public Class<LockingClause> nodeType() {
        return LockingClause.class;
    }

    @Override
    public void validate(LockingClause node, SchemaValidationContext context) {
        validateFeature(context, node, featureForMode(node.mode()), "locking.mode");
        if (!node.ofTables().isEmpty()) {
            validateFeature(context, node, SqlFeature.LOCKING_OF, "locking.of");
        }
        switch (node.waitMode()) {
            case DEFAULT -> {
            }
            case NOWAIT -> validateFeature(context, node, SqlFeature.LOCKING_NOWAIT, "locking.wait");
            case SKIP_LOCKED -> validateFeature(context, node, SqlFeature.LOCKING_SKIP_LOCKED, "locking.wait");
            case WAIT -> validateFeature(context, node, SqlFeature.LOCKING_WAIT_TIMEOUT, "locking.wait");
        }
    }

    private static SqlFeature featureForMode(LockMode mode) {
        return switch (mode) {
            case UPDATE -> SqlFeature.LOCKING_CLAUSE;
            case SHARE -> SqlFeature.LOCKING_SHARE;
            case KEY_SHARE -> SqlFeature.LOCKING_KEY_SHARE;
            case NO_KEY_UPDATE -> SqlFeature.LOCKING_NO_KEY_UPDATE;
        };
    }

    private void validateFeature(SchemaValidationContext context, LockingClause node, SqlFeature feature, String clausePath) {
        if (capabilities.supports(feature)) {
            return;
        }
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            dialectName + " " + version + " does not support " + feature.description(),
            node,
            clausePath
        );
    }
}
