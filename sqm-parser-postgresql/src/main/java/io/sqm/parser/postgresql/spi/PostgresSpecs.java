package io.sqm.parser.postgresql.spi;

import io.sqm.parser.ansi.AnsiLookups;
import io.sqm.parser.ansi.AnsiOperatorPolicy;
import io.sqm.parser.postgresql.Parsers;
import io.sqm.parser.spi.*;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;

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
            var pg90 = SqlDialectVersion.of(9, 0);
            var pg93 = SqlDialectVersion.of(9, 3);
            var pg94 = SqlDialectVersion.of(9, 4);
            var pg95 = SqlDialectVersion.of(9, 5);
            var pg12 = SqlDialectVersion.of(12, 0);
            capabilities = VersionedDialectCapabilities.builder(version)
                .supports(pg90,
                    SqlFeature.DATE_TYPED_LITERAL,
                    SqlFeature.TIME_TYPED_LITERAL,
                    SqlFeature.TIMESTAMP_TYPED_LITERAL,
                    SqlFeature.INTERVAL_LITERAL,
                    SqlFeature.BIT_STRING_LITERAL,
                    SqlFeature.HEX_STRING_LITERAL,
                    SqlFeature.ESCAPE_STRING_LITERAL,
                    SqlFeature.DOLLAR_STRING_LITERAL,
                    SqlFeature.DISTINCT_ON,
                    SqlFeature.ORDER_BY_USING,
                    SqlFeature.LOCKING_CLAUSE,
                    SqlFeature.LOCKING_SHARE,
                    SqlFeature.LOCKING_OF,
                    SqlFeature.LOCKING_NOWAIT,
                    SqlFeature.TABLE_INHERITANCE_ONLY,
                    SqlFeature.TABLE_INHERITANCE_DESCENDANTS,
                    SqlFeature.FUNCTION_TABLE,
                    SqlFeature.ILIKE_PREDICATE,
                    SqlFeature.SIMILAR_TO_PREDICATE,
                    SqlFeature.IS_DISTINCT_FROM_PREDICATE,
                    SqlFeature.REGEX_PREDICATE,
                    SqlFeature.POSTGRES_TYPECAST,
                    SqlFeature.ARRAY_LITERAL,
                    SqlFeature.ARRAY_SUBSCRIPT,
                    SqlFeature.ARRAY_SLICE,
                    SqlFeature.CUSTOM_OPERATOR,
                    SqlFeature.AT_TIME_ZONE,
                    SqlFeature.EXPR_COLLATE,
                    SqlFeature.EXPONENTIATION_OPERATOR
                )
                .supports(pg93,
                    SqlFeature.LATERAL,
                    SqlFeature.LOCKING_KEY_SHARE,
                    SqlFeature.LOCKING_NO_KEY_UPDATE
                )
                .supports(pg94, SqlFeature.FUNCTION_TABLE_ORDINALITY)
                .supports(pg95,
                    SqlFeature.GROUPING_SETS,
                    SqlFeature.ROLLUP,
                    SqlFeature.CUBE,
                    SqlFeature.LOCKING_SKIP_LOCKED
                )
                .supports(pg12, SqlFeature.CTE_MATERIALIZATION)
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
            operatorPolicy = new PostgresOperatorPolicy(new AnsiOperatorPolicy());
        }
        return operatorPolicy;
    }
}
