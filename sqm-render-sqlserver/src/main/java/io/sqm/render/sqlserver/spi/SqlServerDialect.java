package io.sqm.render.sqlserver.spi;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.sqlserver.dialect.SqlServerCapabilities;
import io.sqm.render.ansi.spi.AnsiNullSorting;
import io.sqm.render.defaults.DefaultOperators;
import io.sqm.render.defaults.DefaultValueFormatter;
import io.sqm.render.spi.Booleans;
import io.sqm.render.spi.IdentifierQuoter;
import io.sqm.render.spi.NullSorting;
import io.sqm.render.spi.Operators;
import io.sqm.render.spi.PaginationStyle;
import io.sqm.render.spi.RenderersRepository;
import io.sqm.render.spi.SqlDialect;
import io.sqm.render.spi.ValueFormatter;
import io.sqm.render.sqlserver.Renderers;

import java.util.Objects;

/**
 * SQL Server SQL rendering dialect implementation.
 */
public class SqlServerDialect implements SqlDialect {

    private final IdentifierQuoter quoter;
    private final ValueFormatter formatter = new DefaultValueFormatter(this);
    private final Operators operators = new DefaultOperators();
    private final Booleans booleans = new SqlServerBooleans();
    private final NullSorting nullSorting = new AnsiNullSorting();
    private final PaginationStyle paginationStyle = new SqlServerPaginationStyle();
    private final RenderersRepository repository = Renderers.sqlServer();
    private final DialectCapabilities capabilities;

    /**
     * Creates a SQL Server dialect for baseline version 2019 with bracket identifier quoting.
     */
    public SqlServerDialect() {
        this(SqlDialectVersion.of(2019, 0), false);
    }

    /**
     * Creates a SQL Server dialect for a specific version with bracket identifier quoting.
     *
     * @param version SQL Server version used to evaluate feature availability.
     */
    public SqlServerDialect(SqlDialectVersion version) {
        this(version, false);
    }

    /**
     * Creates a SQL Server dialect for a specific version and quoted-identifier mode.
     *
     * @param version              SQL Server version used to evaluate feature availability.
     * @param quotedIdentifierMode if {@code true}, double-quoted identifiers are supported by the quoter.
     */
    public SqlServerDialect(SqlDialectVersion version, boolean quotedIdentifierMode) {
        Objects.requireNonNull(version, "version");
        this.capabilities = SqlServerCapabilities.of(version);
        this.quoter = new SqlServerIdentifierQuoter(quotedIdentifierMode);
    }

    /**
     * Returns the dialect name.
     *
     * @return dialect name.
     */
    @Override
    public String name() {
        return "SQL Server";
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
        return repository;
    }
}
