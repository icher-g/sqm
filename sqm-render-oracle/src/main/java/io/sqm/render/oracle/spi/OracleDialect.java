package io.sqm.render.oracle.spi;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.oracle.dialect.OracleCapabilities;
import io.sqm.render.ansi.spi.AnsiNullSorting;
import io.sqm.render.defaults.DefaultOperators;
import io.sqm.render.defaults.DefaultValueFormatter;
import io.sqm.render.oracle.Renderers;
import io.sqm.render.spi.Booleans;
import io.sqm.render.spi.IdentifierQuoter;
import io.sqm.render.spi.NullSorting;
import io.sqm.render.spi.Operators;
import io.sqm.render.spi.PaginationStyle;
import io.sqm.render.spi.RenderersRepository;
import io.sqm.render.spi.SqlDialect;
import io.sqm.render.spi.ValueFormatter;

import java.util.Objects;

/**
 * Oracle SQL rendering dialect implementation.
 */
public class OracleDialect implements SqlDialect {
    private final IdentifierQuoter quoter = new OracleIdentifierQuoter();
    private final ValueFormatter formatter = new DefaultValueFormatter(this);
    private final Operators operators = new DefaultOperators();
    private final Booleans booleans = new OracleBooleans();
    private final NullSorting nullSorting = new AnsiNullSorting();
    private final PaginationStyle paginationStyle = new OraclePaginationStyle();
    private final RenderersRepository repository = Renderers.oracle();
    private final DialectCapabilities capabilities;

    /**
     * Creates an Oracle dialect for baseline version 19c.
     */
    public OracleDialect() {
        this(SqlDialectVersion.of(19, 0));
    }

    /**
     * Creates an Oracle dialect for a specific version.
     *
     * @param version Oracle version used to evaluate feature availability.
     */
    public OracleDialect(SqlDialectVersion version) {
        Objects.requireNonNull(version, "version");
        this.capabilities = OracleCapabilities.of(version);
    }

    /**
     * Returns the dialect name.
     *
     * @return dialect name.
     */
    @Override
    public String name() {
        return "Oracle";
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
    public boolean usesAsForTableAliases() {
        return false;
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
