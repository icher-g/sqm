package io.sqm.validate.schema.rule;

import io.sqm.core.SampledTable;
import io.sqm.core.Table;
import io.sqm.core.TablePartitionSpec;
import io.sqm.core.TableRef;
import io.sqm.core.TableSampleSpec;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.Objects;

/**
 * Validates dialect support for table access modifiers.
 */
public final class TableAccessFeatureValidationRule implements SchemaValidationRule<TableRef> {
    private final String dialectName;
    private final SqlDialectVersion version;
    private final DialectCapabilities capabilities;

    /**
     * Creates a table access feature validation rule.
     *
     * @param dialectName dialect name used in validation messages
     * @param version dialect version used in validation messages
     * @param capabilities dialect capabilities
     */
    public TableAccessFeatureValidationRule(String dialectName, SqlDialectVersion version, DialectCapabilities capabilities) {
        this.dialectName = Objects.requireNonNull(dialectName, "dialectName");
        this.version = Objects.requireNonNull(version, "version");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    @Override
    public Class<TableRef> nodeType() {
        return TableRef.class;
    }

    @Override
    public void validate(TableRef node, SchemaValidationContext context) {
        if (node instanceof Table table) {
            if (table.version() != null) {
                require(context, node, SqlFeature.TABLE_VERSIONING, "from.tableVersion");
            }
            if (table.partitionSpec() != null) {
                require(context, node, SqlFeature.TABLE_PARTITION_SPEC, "from.tablePartition");
                if ("mysql".equalsIgnoreCase(dialectName) && table.partitionSpec().kind() == TablePartitionSpec.TablePartitionSpecKind.SUBPARTITION) {
                    unsupported(context, node, "MySQL does not support SUBPARTITION table selectors", "from.tablePartition");
                }
            }
        }
        if (node instanceof SampledTable sampledTable) {
            require(context, node, SqlFeature.TABLE_SAMPLE, "from.tableSample");
            if ("postgresql".equalsIgnoreCase(dialectName)
                && sampledTable.sampleSpec().method() != TableSampleSpec.SampleMethod.BERNOULLI
                && sampledTable.sampleSpec().method() != TableSampleSpec.SampleMethod.SYSTEM) {
                unsupported(context, node, "PostgreSQL supports only BERNOULLI and SYSTEM table sampling", "from.tableSample");
            }
        }
    }

    private void require(SchemaValidationContext context, TableRef node, SqlFeature feature, String clausePath) {
        if (!capabilities.supports(feature)) {
            unsupported(context, node, dialectName + " " + version + " does not support " + feature.description(), clausePath);
        }
    }

    private void unsupported(SchemaValidationContext context, TableRef node, String message, String clausePath) {
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            message,
            node,
            clausePath
        );
    }
}
