package io.sqm.validate.schema.rule;

import io.sqm.core.PivotTable;
import io.sqm.core.TableRef;
import io.sqm.core.UnpivotTable;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.Objects;

/**
 * Validates dialect support for pivot and unpivot table transforms.
 */
public final class PivotFeatureValidationRule implements SchemaValidationRule<io.sqm.core.TableRef> {
    private final String dialectName;
    private final SqlDialectVersion version;
    private final DialectCapabilities capabilities;

    /**
     * Creates a pivot feature validation rule.
     *
     * @param dialectName dialect name used in validation messages
     * @param version dialect version used in validation messages
     * @param capabilities dialect capabilities
     */
    public PivotFeatureValidationRule(String dialectName, SqlDialectVersion version, DialectCapabilities capabilities) {
        this.dialectName = Objects.requireNonNull(dialectName, "dialectName");
        this.version = Objects.requireNonNull(version, "version");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    /**
     * Returns the node type this rule validates.
     *
     * @return table reference class
     */
    @Override
    public Class<TableRef> nodeType() {
        return TableRef.class;
    }

    /**
     * Validates dialect support for pivot/unpivot table references.
     *
     * @param node table reference
     * @param context validation context
     */
    @Override
    public void validate(io.sqm.core.TableRef node, SchemaValidationContext context) {
        if (node instanceof PivotTable && !capabilities.supports(SqlFeature.PIVOT_TABLE)) {
            unsupported(context, node, SqlFeature.PIVOT_TABLE, "from.pivot");
        }
        if (node instanceof UnpivotTable && !capabilities.supports(SqlFeature.UNPIVOT_TABLE)) {
            unsupported(context, node, SqlFeature.UNPIVOT_TABLE, "from.unpivot");
        }
    }

    private void unsupported(SchemaValidationContext context, io.sqm.core.Node node, SqlFeature feature, String path) {
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            dialectName + " " + version + " does not support " + feature.description(),
            node,
            path
        );
    }
}
