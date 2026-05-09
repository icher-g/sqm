package io.sqm.validate.schema.rule;

import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.ValidationCatalogSchemas;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class HierarchicalQueryFeatureValidationRuleTest {
    private static final SqlDialectVersion VERSION = SqlDialectVersion.of(1);

    @Test
    void exposesHierarchicalQueryNodeType() {
        assertEquals(io.sqm.core.HierarchicalQueryClause.class, supportedRule().nodeType());
    }

    @Test
    void reportsUnsupportedHierarchicalQueryFeature() {
        var context = context();

        unsupportedRule().validate(hierarchy(null, prior(col("id")).eq(col("parent_id")), false), context);

        assertEquals(1, context.problems().size());
        var problem = context.problems().getFirst();
        assertEquals(ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED, problem.code());
        assertEquals("query.hierarchical", problem.clausePath());
    }

    @Test
    void acceptsSupportedHierarchicalQueryFeature() {
        var context = context();

        supportedRule().validate(hierarchy(null, prior(col("id")).eq(col("parent_id")), false), context);

        assertTrue(context.problems().isEmpty());
    }

    private static HierarchicalQueryFeatureValidationRule unsupportedRule() {
        return new HierarchicalQueryFeatureValidationRule(
            "test",
            VERSION,
            VersionedDialectCapabilities.builder(VERSION).build()
        );
    }

    private static HierarchicalQueryFeatureValidationRule supportedRule() {
        return new HierarchicalQueryFeatureValidationRule(
            "test",
            VERSION,
            VersionedDialectCapabilities.builder(VERSION)
                .supports(SqlFeature.HIERARCHICAL_QUERY)
                .build()
        );
    }

    private static SchemaValidationContext context() {
        return new SchemaValidationContext(ValidationCatalogSchemas.allowEverything());
    }
}
