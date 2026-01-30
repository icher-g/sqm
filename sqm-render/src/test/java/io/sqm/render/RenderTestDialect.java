package io.sqm.render;

import io.sqm.core.Direction;
import io.sqm.core.Nulls;
import io.sqm.render.defaults.DefaultOperators;
import io.sqm.render.defaults.DefaultValueFormatter;
import io.sqm.render.repos.DefaultRenderersRepository;
import io.sqm.render.spi.*;

public class RenderTestDialect implements SqlDialect {

    private final RenderersRepository renderers = new DefaultRenderersRepository();
    private final IdentifierQuoter quoter = new SimpleQuoter();
    private final Operators operators = new DefaultOperators();
    private final Booleans booleans = new SimpleBooleans();
    private final NullSorting nullSorting = new SimpleNullSorting();
    private final PaginationStyle paginationStyle = new SimplePaginationStyle();
    private final ValueFormatter formatter = new DefaultValueFormatter(this);

    public RenderTestDialect register(Renderer<?> renderer) {
        renderers.register(renderer);
        return this;
    }

    @Override
    public String name() {
        return "test";
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
        return renderers;
    }

    private static final class SimpleQuoter implements IdentifierQuoter {
        @Override
        public String quote(String identifier) {
            return "\"" + identifier + "\"";
        }

        @Override
        public String quoteIfNeeded(String identifier) {
            return quote(identifier);
        }

        @Override
        public String qualify(String schemaOrNull, String name) {
            if (schemaOrNull == null || schemaOrNull.isBlank()) {
                return quote(name);
            }
            return quote(schemaOrNull) + "." + quote(name);
        }

        @Override
        public boolean needsQuoting(String identifier) {
            return true;
        }
    }

    private static final class SimpleBooleans implements Booleans {
        @Override
        public String trueLiteral() {
            return "TRUE";
        }

        @Override
        public String falseLiteral() {
            return "FALSE";
        }

        @Override
        public boolean requireExplicitPredicate() {
            return false;
        }
    }

    private static final class SimpleNullSorting implements NullSorting {
        @Override
        public boolean supportsExplicit() {
            return true;
        }

        @Override
        public String keyword(Nulls n) {
            return n == Nulls.FIRST ? "NULLS FIRST" : "NULLS LAST";
        }

        @Override
        public Nulls defaultFor(Direction dir) {
            return dir == Direction.ASC ? Nulls.LAST : Nulls.FIRST;
        }
    }

    private static final class SimplePaginationStyle implements PaginationStyle {
        @Override
        public boolean supportsLimitOffset() {
            return true;
        }

        @Override
        public boolean supportsOffsetFetch() {
            return true;
        }

        @Override
        public boolean supportsTop() {
            return false;
        }
    }
}
