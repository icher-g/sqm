package io.sqm.validate.schema.rule;

import io.sqm.core.TableRef;
import io.sqm.core.TableSampleSpec;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.ValidationCatalogSchemas;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableAccessFeatureValidationRuleTest {
    private static final SqlDialectVersion VERSION = SqlDialectVersion.of(1);

    @Test
    void exposesTableRefNodeType() {
        assertEquals(TableRef.class, supportedRule("test").nodeType());
    }

    @Test
    void reportsUnsupportedVersionPartitionAndSampleFeatures() {
        var context = context();
        var rule = unsupportedRule("test");

        rule.validate(tbl("orders").withVersion(asOfTimestamp(lit(42))), context);
        rule.validate(tbl("sales").withPartitionSpec(tablePartition("sales_q1")), context);
        rule.validate(sampled(
            tbl("users"),
            tableSample(TableSampleSpec.SampleMethod.DIALECT_DEFAULT, TableSampleSpec.SampleUnit.PERCENT, lit(10), null)), context);

        assertEquals(3, context.problems().size());
        assertEquals("from.tableVersion", context.problems().getFirst().clausePath());
        assertEquals("from.tablePartition", context.problems().get(1).clausePath());
        assertEquals("from.tableSample", context.problems().get(2).clausePath());
        assertTrue(context.problems().stream().allMatch(problem -> problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED));
    }

    @Test
    void acceptsSupportedTableAccessModifiersAndIgnoresPlainTables() {
        var context = context();
        var rule = supportedRule("test");

        rule.validate(tbl("orders").withVersion(asOfScn(lit(42))), context);
        rule.validate(tbl("sales").withPartitionSpec(tablePartition("sales_q1")), context);
        rule.validate(sampled(
            tbl("users"),
            tableSample(TableSampleSpec.SampleMethod.BERNOULLI, TableSampleSpec.SampleUnit.PERCENT, lit(10), lit(42))), context);
        rule.validate(tbl("plain"), context);

        assertTrue(context.problems().isEmpty());
    }

    @Test
    void reportsMysqlSubpartitionAsUnsupportedEvenWhenPartitionsAreSupported() {
        var context = context();
        var rule = supportedRule("mysql");

        rule.validate(tbl("sales").withPartitionSpec(subpartition("sales_q1_eu")), context);

        assertEquals(1, context.problems().size());
        assertEquals("from.tablePartition", context.problems().getFirst().clausePath());
        assertTrue(context.problems().getFirst().message().contains("SUBPARTITION"));
    }

    @Test
    void reportsUnsupportedPostgresSampleMethod() {
        var context = context();
        var rule = supportedRule("postgresql");

        rule.validate(sampled(
            tbl("users"),
            tableSample(TableSampleSpec.SampleMethod.BLOCK, TableSampleSpec.SampleUnit.PERCENT, lit(10), null)), context);

        assertEquals(1, context.problems().size());
        assertEquals("from.tableSample", context.problems().getFirst().clausePath());
        assertTrue(context.problems().getFirst().message().contains("BERNOULLI"));
    }

    private static TableAccessFeatureValidationRule unsupportedRule(String dialectName) {
        return new TableAccessFeatureValidationRule(
            dialectName,
            VERSION,
            VersionedDialectCapabilities.builder(VERSION).build()
        );
    }

    private static TableAccessFeatureValidationRule supportedRule(String dialectName) {
        return new TableAccessFeatureValidationRule(
            dialectName,
            VERSION,
            VersionedDialectCapabilities.builder(VERSION)
                .supports(SqlFeature.TABLE_VERSIONING)
                .supports(SqlFeature.TABLE_PARTITION_SPEC)
                .supports(SqlFeature.TABLE_SAMPLE)
                .build()
        );
    }

    private static SchemaValidationContext context() {
        return new SchemaValidationContext(ValidationCatalogSchemas.allowEverything());
    }
}
