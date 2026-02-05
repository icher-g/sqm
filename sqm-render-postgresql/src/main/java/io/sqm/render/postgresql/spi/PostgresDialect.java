package io.sqm.render.postgresql.spi;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.render.postgresql.Renderers;
import io.sqm.render.spi.*;

public class PostgresDialect implements SqlDialect {

    private final IdentifierQuoter quoter = new PostgresIdentifierQuoter();
    private final ValueFormatter formatter = new PostgresValueFormatter(this);
    private final Operators operators = new PostgresOperators();
    private final Booleans booleans = new PostgresBooleans();
    private final NullSorting nullSorting = new PostgresNullSorting();
    private final PaginationStyle paginationStyle = new PostgresPaginationStyle();
    private final RenderersRepository repo = Renderers.postgres();
    private final DialectCapabilities capabilities;

    /**
     * Creates a PostgreSQL dialect for the latest supported major version.
     */
    public PostgresDialect() {
        this(SqlDialectVersion.of(18, 0));
    }

    /**
     * Creates a PostgreSQL dialect for a specific version.
     *
     * @param version PostgreSQL version used to evaluate feature availability
     */
    public PostgresDialect(SqlDialectVersion version) {
        this.capabilities = buildCapabilities(version);
    }

    /**
     * The name of the dialect. Mostly for the debugging/logging purposes.
     *
     * @return a name of the dialect.
     */
    @Override
    public String name() {
        return "PostgreSQL";
    }

    /**
     * Gets an identifier quoter that decides how identifiers are quoted/qualified.
     *
     * @return a quoter.
     */
    @Override
    public IdentifierQuoter quoter() {
        return quoter;
    }

    /**
     * Gets a value formatter that formats a value into a string.
     *
     * @return a formatter.
     */
    @Override
    public ValueFormatter formatter() {
        return formatter;
    }

    /**
     * Gets operators that customize tokens for arithmetic/comparison/string ops.
     *
     * @return operators.
     */
    @Override
    public Operators operators() {
        return operators;
    }

    /**
     * Gets booleans that define boolean literals & predicate rules.
     *
     * @return booleans.
     */
    @Override
    public Booleans booleans() {
        return booleans;
    }

    /**
     * Gets null sorting definition that provides null ordering policy and emulation.
     *
     * @return null sorting.
     */
    @Override
    public NullSorting nullSorting() {
        return nullSorting;
    }

    /**
     * Gets a pagination style definition.
     *
     * @return a pagination style.
     */
    @Override
    public PaginationStyle paginationStyle() {
        return paginationStyle;
    }

    @Override
    public DialectCapabilities capabilities() {
        return capabilities;
    }

    /**
     * Gets renderers repository.
     *
     * @return a repository.
     */
    @Override
    public RenderersRepository renderers() {
        return repo;
    }

    private static DialectCapabilities buildCapabilities(SqlDialectVersion version) {
        var pg90 = SqlDialectVersion.of(9, 0);
        var pg93 = SqlDialectVersion.of(9, 3);
        var pg94 = SqlDialectVersion.of(9, 4);
        var pg95 = SqlDialectVersion.of(9, 5);
        var pg12 = SqlDialectVersion.of(12, 0);
        return VersionedDialectCapabilities.builder(version)
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
                SqlFeature.EXPR_COLLATE,
                SqlFeature.AT_TIME_ZONE,
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
}
