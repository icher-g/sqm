package io.sqm.validate.mysql;

import io.sqm.core.Node;
import io.sqm.validate.mysql.rule.MySqlIndexHintValidationRule;
import io.sqm.validate.schema.dialect.SchemaValidationDialect;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.List;

/**
 * MySQL-specific schema validation dialect.
 *
 * <p>This dialect currently contributes optional validation for conflicting
 * MySQL table index-hint combinations.</p>
 */
public final class MySqlValidationDialect implements SchemaValidationDialect {
    private static final MySqlValidationDialect INSTANCE = new MySqlValidationDialect();

    private MySqlValidationDialect() {
    }

    /**
     * Returns the shared MySQL validation dialect instance.
     *
     * @return MySQL validation dialect.
     */
    public static MySqlValidationDialect of() {
        return INSTANCE;
    }

    /**
     * Returns the dialect name used by validation callers.
     *
     * @return dialect name.
     */
    @Override
    public String name() {
        return "mysql";
    }

    /**
     * Returns MySQL-specific validation rules.
     *
     * @return immutable list of additional rules.
     */
    @Override
    public List<SchemaValidationRule<? extends Node>> additionalRules() {
        return List.of(new MySqlIndexHintValidationRule());
    }
}
