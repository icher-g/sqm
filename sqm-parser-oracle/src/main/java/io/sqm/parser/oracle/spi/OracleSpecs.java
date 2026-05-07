package io.sqm.parser.oracle.spi;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.oracle.dialect.OracleCapabilities;
import io.sqm.parser.ansi.AnsiLookups;
import io.sqm.parser.ansi.AnsiOperatorPolicy;
import io.sqm.parser.oracle.Parsers;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.Lookups;
import io.sqm.parser.spi.OperatorPolicy;
import io.sqm.parser.spi.ParsersRepository;
import io.sqm.parser.spi.Specs;

import java.util.Objects;

/**
 * Oracle parser specifications, including parser registrations, identifier
 * quoting rules, and feature-gating capabilities.
 */
public class OracleSpecs implements Specs {
    private final SqlDialectVersion version;
    private Lookups lookups;
    private IdentifierQuoting identifierQuoting;
    private DialectCapabilities capabilities;
    private OperatorPolicy operatorPolicy;

    /**
     * Creates Oracle specs for the baseline 19c version.
     */
    public OracleSpecs() {
        this(SqlDialectVersion.of(19, 0));
    }

    /**
     * Creates Oracle specs for a specific dialect version.
     *
     * @param version Oracle version used to evaluate feature availability.
     */
    public OracleSpecs(SqlDialectVersion version) {
        this.version = Objects.requireNonNull(version, "version");
    }

    /**
     * Gets a parser repository.
     *
     * @return parser repository.
     */
    @Override
    public ParsersRepository parsers() {
        return Parsers.oracle();
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
     * Returns identifier quoting rules supported by this Oracle parser.
     *
     * @return Oracle identifier quoting configuration.
     */
    @Override
    public IdentifierQuoting identifierQuoting() {
        if (identifierQuoting == null) {
            identifierQuoting = IdentifierQuoting.of('"');
        }
        return identifierQuoting;
    }

    /**
     * Returns Oracle dialect capabilities used for parser feature gating.
     *
     * @return Oracle dialect capabilities.
     */
    @Override
    public DialectCapabilities capabilities() {
        if (capabilities == null) {
            capabilities = OracleCapabilities.of(version);
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
