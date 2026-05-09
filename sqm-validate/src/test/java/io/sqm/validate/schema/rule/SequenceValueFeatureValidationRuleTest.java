package io.sqm.validate.schema.rule;

import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.ValidationCatalogSchemas;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.currentValue;
import static io.sqm.dsl.Dsl.nextValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SequenceValueFeatureValidationRuleTest {
    private static final SqlDialectVersion VERSION = SqlDialectVersion.of(1);

    @Test
    void exposesSequenceValueNodeType() {
        assertEquals(io.sqm.core.SequenceValueExpr.class, supportedRule(true).nodeType());
    }

    @Test
    void reportsUnsupportedSequenceValueFeature() {
        var context = context();

        unsupportedRule().validate(nextValue("users_seq"), context);

        assertEquals(1, context.problems().size());
        var problem = context.problems().getFirst();
        assertEquals(ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED, problem.code());
        assertEquals("expression.sequence_value", problem.clausePath());
    }

    @Test
    void reportsUnsupportedCurrentSequenceValue() {
        var context = context();

        supportedRule(false).validate(currentValue("users_seq"), context);

        assertEquals(1, context.problems().size());
        assertEquals(ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED, context.problems().getFirst().code());
    }

    @Test
    void acceptsSupportedSequenceValueKinds() {
        var context = context();
        var rule = supportedRule(true);

        rule.validate(nextValue("users_seq"), context);
        rule.validate(currentValue("users_seq"), context);

        assertTrue(context.problems().isEmpty());
    }

    private static SequenceValueFeatureValidationRule unsupportedRule() {
        return new SequenceValueFeatureValidationRule(
            "test",
            VERSION,
            VersionedDialectCapabilities.builder(VERSION).build(),
            true
        );
    }

    private static SequenceValueFeatureValidationRule supportedRule(boolean currentValueSupported) {
        return new SequenceValueFeatureValidationRule(
            "test",
            VERSION,
            VersionedDialectCapabilities.builder(VERSION)
                .supports(SqlFeature.SEQUENCE_VALUE_EXPRESSION)
                .build(),
            currentValueSupported
        );
    }

    private static SchemaValidationContext context() {
        return new SchemaValidationContext(ValidationCatalogSchemas.allowEverything());
    }
}
