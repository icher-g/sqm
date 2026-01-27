package io.sqm.render.pgsql.spi;

import io.sqm.render.pgsql.Renderers;
import io.sqm.render.spi.*;

public class PostgresDialect implements SqlDialect {

    private final IdentifierQuoter quoter = new PostgresIdentifierQuoter();
    private final ValueFormatter formatter = new PostgresValueFormatter(this);
    private final Operators operators = new PostgresOperators();
    private final Booleans booleans = new PostgresBooleans();
    private final NullSorting nullSorting = new PostgresNullSorting();
    private final PaginationStyle paginationStyle = new PostgresPaginationStyle();
    private final RenderersRepository repo = Renderers.postgres();

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

    /**
     * Gets renderers repository.
     *
     * @return a repository.
     */
    @Override
    public RenderersRepository renderers() {
        return repo;
    }
}
