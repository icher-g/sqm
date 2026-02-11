package io.sqm.validate.schema.dialect;

import io.sqm.core.Node;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.function.DefaultFunctionCatalog;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.List;

/**
 * Dialect extension contract for schema validation.
 *
 * <p>Implementations can provide dialect-specific function signatures and
 * additional node validation rules.</p>
 */
public interface SchemaValidationDialect {
    /**
     * Returns dialect name used for diagnostics and logging.
     *
     * @return dialect name.
     */
    String name();

    /**
     * Returns dialect-specific function catalog.
     *
     * @return function catalog.
     */
    default FunctionCatalog functionCatalog() {
        return DefaultFunctionCatalog.standard();
    }

    /**
     * Returns additional dialect-specific validation rules.
     *
     * @return additional rules to append after default rules.
     */
    default List<SchemaValidationRule<? extends Node>> additionalRules() {
        return List.of();
    }

    /**
     * Converts dialect extension to reusable validator settings.
     *
     * @return settings derived from this dialect.
     */
    default SchemaValidationSettings toSettings() {
        return SchemaValidationSettings.builder()
            .functionCatalog(functionCatalog())
            .addRules(additionalRules())
            .build();
    }
}
