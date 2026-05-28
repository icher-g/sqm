package io.sqm.validate.schema.rule;

import io.sqm.core.LockMode;
import io.sqm.core.LockWaitMode;
import io.sqm.core.LockingClause;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.ValidationCatalogSchemas;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.ofTables;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockingClauseFeatureValidationRuleTest {
    private static final SqlDialectVersion VERSION = SqlDialectVersion.of(1);

    @Test
    void exposesLockingClauseNodeType() {
        assertEquals(LockingClause.class, supportedRule().nodeType());
    }

    @Test
    void reportsUnsupportedLockingFeatures() {
        var context = context();
        var rule = unsupportedRule();

        rule.validate(LockingClause.of(LockMode.UPDATE, ofTables("u"), LockWaitMode.WAIT, lit(5)), context);

        assertEquals(3, context.problems().size());
        assertTrue(context.problems().stream().allMatch(problem -> problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED));
        assertEquals("locking.mode", context.problems().getFirst().clausePath());
        assertEquals("locking.of", context.problems().get(1).clausePath());
        assertEquals("locking.wait", context.problems().get(2).clausePath());
    }

    @Test
    void acceptsSupportedLockingFeatures() {
        var context = context();

        supportedRule().validate(LockingClause.of(LockMode.UPDATE, ofTables("u"), LockWaitMode.WAIT, lit(5)), context);

        assertTrue(context.problems().isEmpty());
    }

    @Test
    void reportsUnsupportedSpecificModesAndWaitPolicies() {
        var context = context();
        var rule = new LockingClauseFeatureValidationRule(
            "test",
            VERSION,
            VersionedDialectCapabilities.builder(VERSION)
                .supports(SqlFeature.LOCKING_CLAUSE)
                .supports(SqlFeature.LOCKING_NOWAIT)
                .build()
        );

        rule.validate(LockingClause.of(LockMode.SHARE, List.of(), false, false), context);
        rule.validate(LockingClause.of(LockMode.UPDATE, List.of(), LockWaitMode.SKIP_LOCKED, null), context);

        assertEquals(2, context.problems().size());
        assertEquals("locking.mode", context.problems().getFirst().clausePath());
        assertEquals("locking.wait", context.problems().get(1).clausePath());
    }

    private static LockingClauseFeatureValidationRule unsupportedRule() {
        return new LockingClauseFeatureValidationRule(
            "test",
            VERSION,
            VersionedDialectCapabilities.builder(VERSION).build()
        );
    }

    private static LockingClauseFeatureValidationRule supportedRule() {
        return new LockingClauseFeatureValidationRule(
            "test",
            VERSION,
            VersionedDialectCapabilities.builder(VERSION)
                .supports(SqlFeature.LOCKING_CLAUSE)
                .supports(SqlFeature.LOCKING_OF)
                .supports(SqlFeature.LOCKING_WAIT_TIMEOUT)
                .build()
        );
    }

    private static SchemaValidationContext context() {
        return new SchemaValidationContext(ValidationCatalogSchemas.allowEverything());
    }
}
