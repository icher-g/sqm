package io.sqm.validate.schema.rule;

import io.sqm.core.JsonTableRef;
import io.sqm.core.Node;
import io.sqm.core.TableRef;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.Objects;

/**
 * Validates dialect support for {@code JSON_TABLE} table references.
 */
public final class JsonTableFeatureValidationRule implements SchemaValidationRule<TableRef> {
    private final String dialectName;
    private final SqlDialectVersion version;
    private final DialectCapabilities capabilities;

    /**
     * Creates a JSON table feature validation rule.
     *
     * @param dialectName dialect name used in validation messages
     * @param version dialect version used in validation messages
     * @param capabilities dialect capabilities
     */
    public JsonTableFeatureValidationRule(String dialectName, SqlDialectVersion version, DialectCapabilities capabilities) {
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
        if (node instanceof JsonTableRef && !capabilities.supports(SqlFeature.JSON_TABLE)) {
            unsupported(context, node);
        }
    }

    private void unsupported(SchemaValidationContext context, Node node) {
        context.addProblem(
            ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            dialectName + " " + version + " does not support " + SqlFeature.JSON_TABLE.description(),
            node,
            "from.jsonTable"
        );
    }
}
