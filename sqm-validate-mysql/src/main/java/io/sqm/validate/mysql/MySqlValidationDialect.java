package io.sqm.validate.mysql;

import io.sqm.core.Node;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.mysql.dialect.MySqlCapabilities;
import io.sqm.validate.mysql.rule.MySqlIndexHintValidationRule;
import io.sqm.validate.mysql.rule.MySqlDmlFeatureValidationRule;
import io.sqm.validate.mysql.rule.MySqlSelectFeatureValidationRule;
import io.sqm.validate.mysql.rule.MySqlStatementHintValidationRule;
import io.sqm.validate.mysql.rule.MySqlTableHintValidationRule;
import io.sqm.validate.schema.dialect.SchemaValidationDialect;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.List;
import java.util.Objects;

/**
 * MySQL-specific schema validation dialect.
 *
 * <p>This dialect contributes first-wave MySQL hint validation plus
 * conflicting table index-hint checks.</p>
 */
public final class MySqlValidationDialect implements SchemaValidationDialect {
    private final SqlDialectVersion version;
    private final DialectCapabilities capabilities;

    private MySqlValidationDialect(SqlDialectVersion version) {
        this.version = Objects.requireNonNull(version, "version");
        this.capabilities = MySqlCapabilities.of(version);
    }

    /**
     * Creates MySQL validation dialect for the latest supported version.
     *
     * @return MySQL validation dialect.
     */
    public static MySqlValidationDialect of() {
        return new MySqlValidationDialect(SqlDialectVersion.of(8, 0, 14));
    }

    /**
     * Creates MySQL validation dialect for a specific version.
     *
     * @param version MySQL version.
     * @return MySQL validation dialect.
     */
    public static MySqlValidationDialect of(SqlDialectVersion version) {
        return new MySqlValidationDialect(version);
    }

    /**
     * Returns MySQL version used for feature checks.
     *
     * @return configured MySQL version.
     */
    public SqlDialectVersion version() {
        return version;
    }

    /**
     * Returns dialect capabilities used by feature-gating rules.
     *
     * @return MySQL capabilities.
     */
    public DialectCapabilities capabilities() {
        return capabilities;
    }

    /**
     * Returns the dialect name used by validation callers.
     *
     * @return dialect name.
     */
    @Override
    public String name() {
        return SqlDialectId.MYSQL.value();
    }

    /**
     * Returns MySQL-specific validation rules.
     *
     * @return immutable list of additional rules.
     */
    @Override
    public List<SchemaValidationRule<? extends Node>> additionalRules() {
        return List.of(
            new MySqlSelectFeatureValidationRule(capabilities, version),
            new MySqlDmlFeatureValidationRule(capabilities, version),
            new MySqlStatementHintValidationRule(capabilities, version),
            new MySqlTableHintValidationRule(),
            new MySqlIndexHintValidationRule()
        );
    }
}
