package io.sqm.validate.schema.rule;

import io.sqm.core.PatternRecognitionTable;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.Objects;

/**
 * Validates dialect and version support for row-pattern recognition.
 */
public final class PatternRecognitionFeatureValidationRule implements SchemaValidationRule<PatternRecognitionTable> {
    private final String dialectName;
    private final SqlDialectVersion version;
    private final DialectCapabilities capabilities;

    /**
     * Creates a row-pattern recognition feature rule.
     *
     * @param dialectName dialect name used in diagnostics.
     * @param version dialect version used in diagnostics.
     * @param capabilities dialect feature capabilities.
     */
    public PatternRecognitionFeatureValidationRule(
        String dialectName,
        SqlDialectVersion version,
        DialectCapabilities capabilities
    ) {
        this.dialectName = Objects.requireNonNull(dialectName, "dialectName");
        this.version = Objects.requireNonNull(version, "version");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    /** {@inheritDoc} */
    @Override
    public Class<PatternRecognitionTable> nodeType() {
        return PatternRecognitionTable.class;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(PatternRecognitionTable node, SchemaValidationContext context) {
        if (!capabilities.supports(SqlFeature.MATCH_RECOGNIZE)) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                dialectName + " " + version + " does not support " + SqlFeature.MATCH_RECOGNIZE.description(),
                node,
                "from.matchRecognize"
            );
        }
    }
}
