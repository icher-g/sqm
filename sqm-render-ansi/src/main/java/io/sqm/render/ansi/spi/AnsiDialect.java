package io.sqm.render.ansi.spi;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.render.defaults.DefaultOperators;
import io.sqm.render.defaults.DefaultValueFormatter;
import io.sqm.render.ansi.Renderers;
import io.sqm.render.spi.*;

public class AnsiDialect implements SqlDialect {

    private final IdentifierQuoter quoter = new AnsiIdentifierQuoter();
    private final ValueFormatter formatter = new DefaultValueFormatter(this);
    private final Operators operators = new DefaultOperators();
    private final Booleans booleans = new AnsiBooleans();
    private final NullSorting nullSorting = new AnsiNullSorting();
    private final PaginationStyle paginationStyle = new AnsiPaginationStyle();
    private final RenderersRepository repo = Renderers.ansi();
    private final DialectCapabilities capabilities;

    /**
     * Creates an ANSI dialect for the SQL:2016 standard.
     */
    public AnsiDialect() {
        this(SqlDialectVersion.of(2016));
    }

    /**
     * Creates an ANSI dialect for a specific version.
     *
     * @param version ANSI version used to evaluate feature availability
     */
    public AnsiDialect(SqlDialectVersion version) {
        this.capabilities = buildCapabilities(version);
    }

    @Override
    public String name() {
        return "ansi";
    }

    @Override
    public IdentifierQuoter quoter() {
        return quoter;
    }

    @Override
    public ValueFormatter formatter() {
        return formatter;
    }

    @Override
    public Operators operators() {
        return operators;
    }

    @Override
    public Booleans booleans() {
        return booleans;
    }

    @Override
    public NullSorting nullSorting() {
        return nullSorting;
    }

    @Override
    public PaginationStyle paginationStyle() {
        return paginationStyle;
    }

    @Override
    public DialectCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public RenderersRepository renderers() {
        return repo;
    }

    private static DialectCapabilities buildCapabilities(SqlDialectVersion version) {
        var sql1992 = SqlDialectVersion.of(1992);
        var sql2008 = SqlDialectVersion.of(2008);
        return VersionedDialectCapabilities.builder(version)
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
            // CUSTOM_OPERATOR is not part of the SQL standard; keep enabled for ANSI renderer extensions.
            .supports(SqlFeature.CUSTOM_OPERATOR)
            .build();
    }
}
