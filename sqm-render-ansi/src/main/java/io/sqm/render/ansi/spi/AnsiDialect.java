package io.sqm.render.ansi.spi;

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
    public RenderersRepository renderers() {
        return repo;
    }
}
