package io.sqm.parser.ansi;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.parser.spi.*;

import java.util.Objects;

public class AnsiSpecs implements Specs {

    private final SqlDialectVersion version;
    private Lookups lookups;
    private IdentifierQuoting identifierQuoting;
    private DialectCapabilities capabilities;
    private OperatorPolicy operatorPolicy;

    /**
     * Creates ANSI specs for the SQL:2016 standard.
     */
    public AnsiSpecs() {
        this(SqlDialectVersion.of(2016));
    }

    /**
     * Creates ANSI specs for a specific dialect version.
     *
     * @param version ANSI version used to evaluate feature availability
     */
    public AnsiSpecs(SqlDialectVersion version) {
        this.version = Objects.requireNonNull(version, "version");
    }

    /**
     * Gets a parser's repository.
     *
     * @return a parser's repository.
     */
    @Override
    public ParsersRepository parsers() {
        return Parsers.ansi();
    }

    /**
     * Gets {@link Lookups} implementation.
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
     * Returns the identifier quoting rules supported by this SQL dialect.
     * <p>
     * The returned {@link IdentifierQuoting} defines how quoted identifiers
     * are recognized and parsed by the lexer, including the opening and
     * closing delimiter characters.
     *
     * @return the dialect-specific identifier quoting configuration.
     */
    @Override
    public IdentifierQuoting identifierQuoting() {
        if (identifierQuoting == null) {
            identifierQuoting = IdentifierQuoting.of('"');
        }
        return identifierQuoting;
    }

    /**
     * Returns the dialect capabilities for ANSI SQL.
     *
     * @return dialect capabilities
     */
    @Override
    public DialectCapabilities capabilities() {
        if (capabilities == null) {
            var sql1992 = SqlDialectVersion.of(1992);
            var sql2008 = SqlDialectVersion.of(2008);
            capabilities = VersionedDialectCapabilities.builder(version)
                .supports(sql1992,
                    SqlFeature.DATE_TYPED_LITERAL,
                    SqlFeature.TIME_TYPED_LITERAL,
                    SqlFeature.TIMESTAMP_TYPED_LITERAL,
                    SqlFeature.INTERVAL_LITERAL,
                    SqlFeature.BIT_STRING_LITERAL,
                    SqlFeature.HEX_STRING_LITERAL,
                    SqlFeature.LOCKING_CLAUSE
                )
                .supports(sql2008, SqlFeature.IS_DISTINCT_FROM_PREDICATE)
                // CUSTOM_OPERATOR is not part of the SQL standard; keep enabled for ANSI parser extensions.
                .supports(SqlFeature.CUSTOM_OPERATOR)
                .build();
        }
        return capabilities;
    }

    /**
     * Returns an operator policy per dialect.
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
