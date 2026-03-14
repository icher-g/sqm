package io.sqm.parser.sqlserver.spi;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.sqlserver.dialect.SqlServerCapabilities;
import io.sqm.parser.ansi.AnsiLookups;
import io.sqm.parser.ansi.AnsiOperatorPolicy;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.Lookups;
import io.sqm.parser.spi.OperatorPolicy;
import io.sqm.parser.spi.ParsersRepository;
import io.sqm.parser.spi.Specs;
import io.sqm.parser.sqlserver.Parsers;

import java.util.Objects;

/**
 * SQL Server parser specifications, including parser registrations, identifier
 * quoting rules, and feature gating capabilities.
 */
public class SqlServerSpecs implements Specs {

    private final SqlDialectVersion version;
    private final boolean quotedIdentifierMode;
    private Lookups lookups;
    private IdentifierQuoting identifierQuoting;
    private DialectCapabilities capabilities;
    private OperatorPolicy operatorPolicy;

    /**
     * Creates SQL Server specs for the baseline 2019 version using bracket identifiers.
     */
    public SqlServerSpecs() {
        this(SqlDialectVersion.of(2019, 0), false);
    }

    /**
     * Creates SQL Server specs for a specific dialect version using bracket identifiers.
     *
     * @param version SQL Server version used to evaluate feature availability.
     */
    public SqlServerSpecs(SqlDialectVersion version) {
        this(version, false);
    }

    /**
     * Creates SQL Server specs for a specific dialect version and quoted-identifier mode.
     *
     * @param version SQL Server version used to evaluate feature availability.
     * @param quotedIdentifierMode if {@code true}, double-quoted identifiers are accepted in addition to brackets.
     */
    public SqlServerSpecs(SqlDialectVersion version, boolean quotedIdentifierMode) {
        this.version = Objects.requireNonNull(version, "version");
        this.quotedIdentifierMode = quotedIdentifierMode;
    }

    /**
     * Gets a parser repository.
     *
     * @return parser repository.
     */
    @Override
    public ParsersRepository parsers() {
        return Parsers.sqlServer();
    }

    /**
     * Gets lookups implementation.
     *
     * @return lookups implementation.
     */
    @Override
    public Lookups lookups() {
        if (lookups == null) {
            lookups = new AnsiLookups();
        }
        return lookups;
    }

    /**
     * Returns identifier quoting rules supported by this SQL Server parser.
     *
     * @return SQL Server identifier quoting configuration.
     */
    @Override
    public IdentifierQuoting identifierQuoting() {
        if (identifierQuoting == null) {
            identifierQuoting = quotedIdentifierMode
                ? IdentifierQuoting.of('[', '"')
                : IdentifierQuoting.of('[');
        }
        return identifierQuoting;
    }

    /**
     * Returns SQL Server dialect capabilities used for parser feature gating.
     *
     * @return SQL Server dialect capabilities.
     */
    @Override
    public DialectCapabilities capabilities() {
        if (capabilities == null) {
            capabilities = SqlServerCapabilities.of(version);
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
