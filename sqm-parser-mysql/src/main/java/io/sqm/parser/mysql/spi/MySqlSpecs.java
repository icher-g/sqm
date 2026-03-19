package io.sqm.parser.mysql.spi;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.mysql.dialect.MySqlCapabilities;
import io.sqm.parser.ansi.AnsiOperatorPolicy;
import io.sqm.parser.mysql.Parsers;
import io.sqm.parser.spi.*;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * MySQL parser specifications, including parser registrations, lookups,
 * identifier quoting rules, and feature gating capabilities.
 */
public class MySqlSpecs implements Specs {

    private final SqlDialectVersion version;
    private final Set<MySqlSqlMode> sqlModes;
    private Lookups lookups;
    private IdentifierQuoting identifierQuoting;
    private DialectCapabilities capabilities;
    private OperatorPolicy operatorPolicy;

    /**
     * Creates MySQL specs for the baseline 8.0 version using backtick identifier quoting.
     */
    public MySqlSpecs() {
        this(SqlDialectVersion.of(8, 0), Set.of());
    }

    /**
     * Creates MySQL specs for a specific dialect version using backtick identifier quoting.
     *
     * @param version MySQL version used to evaluate feature availability.
     */
    public MySqlSpecs(SqlDialectVersion version) {
        this(version, Set.of());
    }

    /**
     * Creates MySQL specs for a specific dialect version and SQL mode set.
     *
     * @param version MySQL version used to evaluate feature availability.
     * @param sqlModes explicit SQL modes that affect parser behavior.
     */
    public MySqlSpecs(SqlDialectVersion version, Set<MySqlSqlMode> sqlModes) {
        this.version = Objects.requireNonNull(version, "version");
        Objects.requireNonNull(sqlModes, "sqlModes");
        var normalizedModes = EnumSet.noneOf(MySqlSqlMode.class);
        normalizedModes.addAll(sqlModes);
        this.sqlModes = Set.copyOf(normalizedModes);
    }

    /**
     * Creates MySQL specs for a specific dialect version and quote mode.
     *
     * @param version        MySQL version used to evaluate feature availability.
     * @param ansiQuotesMode if {@code true}, double-quote identifiers are accepted in addition to backticks.
     */
    public MySqlSpecs(SqlDialectVersion version, boolean ansiQuotesMode) {
        this(version, ansiQuotesMode ? Set.of(MySqlSqlMode.ANSI_QUOTES) : Set.of());
    }

    /**
     * Gets a parser's repository.
     *
     * @return a parser repository.
     */
    @Override
    public ParsersRepository parsers() {
        return Parsers.mysql();
    }

    /**
     * Gets {@link Lookups} implementation.
     *
     * @return lookups implementation.
     */
    @Override
    public Lookups lookups() {
        if (lookups == null) {
            lookups = new MySqlLookups();
        }
        return lookups;
    }

    /**
     * Returns identifier quoting rules supported by this MySQL parser.
     *
     * @return MySQL identifier quoting configuration.
     */
    @Override
    public IdentifierQuoting identifierQuoting() {
        if (identifierQuoting == null) {
            identifierQuoting = sqlModes.contains(MySqlSqlMode.ANSI_QUOTES)
                ? IdentifierQuoting.of('`', '"')
                : IdentifierQuoting.of('`');
        }
        return identifierQuoting;
    }

    /**
     * Returns the explicit SQL modes that affect parser behavior.
     *
     * @return immutable SQL mode set.
     */
    public Set<MySqlSqlMode> sqlModes() {
        return sqlModes;
    }

    /**
     * Returns MySQL dialect capabilities used for parser feature gating.
     *
     * @return MySQL dialect capabilities.
     */
    @Override
    public DialectCapabilities capabilities() {
        if (capabilities == null) {
            capabilities = MySqlCapabilities.of(version);
        }
        return capabilities;
    }

    /**
     * Returns operator policy used by the parser.
     *
     * @return operator policy.
     */
    @Override
    public OperatorPolicy operatorPolicy() {
        if (operatorPolicy == null) {
            operatorPolicy = new AnsiOperatorPolicy();
        }
        return operatorPolicy;
    }
}
