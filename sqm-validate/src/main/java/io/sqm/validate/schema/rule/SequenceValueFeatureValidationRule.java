package io.sqm.validate.schema.rule;

import io.sqm.core.SequenceValueExpr;
import io.sqm.core.SequenceValueKind;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.Objects;

/**
 * Validates dialect support for sequence value expressions.
 */
public final class SequenceValueFeatureValidationRule implements SchemaValidationRule<SequenceValueExpr> {
    private final String dialectName;
    private final SqlDialectVersion version;
    private final DialectCapabilities capabilities;
    private final boolean currentValueSupported;

    /**
     * Creates a sequence value feature validation rule.
     *
     * @param dialectName dialect name used in validation messages
     * @param version dialect version used in validation messages
     * @param capabilities dialect capabilities
     * @param currentValueSupported whether current sequence values are supported
     */
    public SequenceValueFeatureValidationRule(
        String dialectName,
        SqlDialectVersion version,
        DialectCapabilities capabilities,
        boolean currentValueSupported
    ) {
        this.dialectName = Objects.requireNonNull(dialectName, "dialectName");
        this.version = Objects.requireNonNull(version, "version");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.currentValueSupported = currentValueSupported;
    }

    @Override
    public Class<SequenceValueExpr> nodeType() {
        return SequenceValueExpr.class;
    }

    @Override
    public void validate(SequenceValueExpr node, SchemaValidationContext context) {
        if (!capabilities.supports(SqlFeature.SEQUENCE_VALUE_EXPRESSION)) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                dialectName + " " + version + " does not support " + SqlFeature.SEQUENCE_VALUE_EXPRESSION.description(),
                node,
                "expression.sequence_value"
            );
            return;
        }
        if (node.kind() == SequenceValueKind.CURRENT_VALUE && !currentValueSupported) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                dialectName + " " + version + " does not support sequence current value expressions",
                node,
                "expression.sequence_value"
            );
        }
    }
}
