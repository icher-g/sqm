package io.cherlabs.sqm.render.ansi.spi;

import io.cherlabs.sqm.render.ansi.Renderers;
import io.cherlabs.sqm.render.spi.SqlDialect;
import io.cherlabs.sqm.render.spi.ValueFormatter;
import io.cherlabs.sqm.render.ansi.DefaultValueFormatter;
import io.cherlabs.sqm.render.spi.*;

public class AnsiDialect implements SqlDialect {

    private final IdentifierQuoter quoter = new AnsiIdentifierQuoter();
    private final ValueFormatter formatter = new DefaultValueFormatter(this);
    private final Placeholders placeholders = new AnsiPlaceholders();
    private final Operators operators = new AnsiOperators();
    private final Booleans booleans = new AnsiBooleans();
    private final NullSorting nullSorting = new AnsiNullSorting();
    private final PaginationStyle paginationStyle = new AnsiPaginationStyle();
    private final RenderersRepository repo = Renderers.defaultRepository();

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
    public Placeholders placeholders() {
        return placeholders;
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
