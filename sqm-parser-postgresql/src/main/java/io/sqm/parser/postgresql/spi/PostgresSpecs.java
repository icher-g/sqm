package io.sqm.parser.postgresql.spi;

import io.sqm.parser.ansi.AnsiLookups;
import io.sqm.parser.ansi.AnsiOperatorPolicy;
import io.sqm.parser.postgresql.Parsers;
import io.sqm.parser.spi.*;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.postgresql.dialect.PostgresCapabilities;

import java.util.Objects;

public class PostgresSpecs implements Specs {

    private Lookups lookups;
    private IdentifierQuoting identifierQuoting;
    private DialectCapabilities capabilities;
    private OperatorPolicy operatorPolicy;
    private final SqlDialectVersion version;

    /**
     * Creates PostgreSQL specs for the latest supported major version.
     */
    public PostgresSpecs() {
        this(SqlDialectVersion.of(18, 0));
    }

    /**
     * Creates PostgreSQL specs for a specific dialect version.
     *
     * @param version PostgreSQL version used to evaluate feature availability
     */
    public PostgresSpecs(SqlDialectVersion version) {
        this.version = Objects.requireNonNull(version, "version");
    }

    /**
     * Gets a parser's repository.
     *
     * @return a parser's repository.
     */
    @Override
    public ParsersRepository parsers() {
        return Parsers.postgres();
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
     * Returns the dialect capabilities for PostgreSQL.
     *
     * @return dialect capabilities
     */
    @Override
    public DialectCapabilities capabilities() {
        if (capabilities == null) {
            capabilities = PostgresCapabilities.of(version);
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
            operatorPolicy = new PostgresOperatorPolicy(new AnsiOperatorPolicy());
        }
        return operatorPolicy;
    }
}
