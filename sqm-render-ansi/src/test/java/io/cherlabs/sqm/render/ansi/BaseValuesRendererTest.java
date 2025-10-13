package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.render.SqlText;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.DefaultParamSink;
import io.cherlabs.sqm.render.ansi.spi.AnsiDialect;
import io.cherlabs.sqm.render.spi.*;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class BaseValuesRendererTest {

    protected AnsiTestDialect ansiDialect;
    protected OrdinalTestDialect ordinalDialect;
    protected NamedTestDialect namedDialect;

    /**
     * Assert SQL + params using your BufferSqlWriter API
     */
    protected static void assertSqlAndParams(SqlWriter w, List<Object> params, String expectedSql, List<Object> expectedParams) {
        SqlText out = w.toText(params);
        assertEquals(expectedSql, out.sql().trim());
        assertEquals(expectedParams, out.params());
    }

    @BeforeEach
    void setupDialects() {
        ansiDialect = new AnsiTestDialect();
        ordinalDialect = new OrdinalTestDialect();
        namedDialect = new NamedTestDialect();
    }

    /**
     * Minimal test RenderContext
     */
    protected static final class TestRenderContext implements RenderContext {
        private final SqlDialect dialect;
        private final PlaceholderPreference pref;
        private final ParamSink params;
        private final ParameterizationMode paramMode;

        TestRenderContext(SqlDialect dialect, PlaceholderPreference pref, ParameterizationMode paramMode) {
            this.dialect = dialect;
            this.pref = pref;
            this.params = new DefaultParamSink();
            this.paramMode = paramMode;
        }

        @Override
        public SqlDialect dialect() {
            return dialect;
        }

        public PlaceholderPreference placeholderPreference() {
            return pref;
        }

        public ParamSink params() {
            return params;
        }

        @Override
        public ParameterizationMode parameterizationMode() {
            return paramMode;
        }
    }

    /**
     * ANSI dialect w/ plain '?'
     */
    protected static final class AnsiTestDialect implements SqlDialect {
        private final SqlDialect dialect = new AnsiDialect();
        private final Placeholders placeholders = () -> "?";

        @Override
        public String name() {
            return dialect.name();
        }

        @Override
        public IdentifierQuoter quoter() {
            return dialect.quoter();
        }

        @Override
        public ValueFormatter formatter() {
            return dialect.formatter();
        }

        @Override
        public Placeholders placeholders() {
            return placeholders;
        }

        @Override
        public Operators operators() {
            return dialect.operators();
        }

        @Override
        public Booleans booleans() {
            return dialect.booleans();
        }

        @Override
        public NullSorting nullSorting() {
            return dialect.nullSorting();
        }

        @Override
        public PaginationStyle paginationStyle() {
            return dialect.paginationStyle();
        }

        @Override
        public RenderersRepository renderers() {
            return dialect.renderers();
        }
    }

    /**
     * PG-like ordinal placeholders $1,$2…
     */
    protected static final class OrdinalTestDialect implements SqlDialect {
        private final SqlDialect dialect = new AnsiDialect();
        private final Placeholders placeholders = new Placeholders() {
            @Override
            public String marker() {
                return "?";
            } // fallback

            @Override
            public boolean supportsOrdinal() {
                return true;
            }

            @Override
            public String ordinal(int position) {
                return "$" + position;
            }
        };

        @Override
        public String name() {
            return dialect.name();
        }

        @Override
        public IdentifierQuoter quoter() {
            return dialect.quoter();
        }

        @Override
        public ValueFormatter formatter() {
            return dialect.formatter();
        }

        @Override
        public Placeholders placeholders() {
            return placeholders;
        }

        @Override
        public Operators operators() {
            return dialect.operators();
        }

        @Override
        public Booleans booleans() {
            return dialect.booleans();
        }

        @Override
        public NullSorting nullSorting() {
            return dialect.nullSorting();
        }

        @Override
        public PaginationStyle paginationStyle() {
            return dialect.paginationStyle();
        }

        @Override
        public RenderersRepository renderers() {
            return dialect.renderers();
        }
    }

    /**
     * Named placeholders :p1,:p2…
     */
    protected static final class NamedTestDialect implements SqlDialect {
        private final SqlDialect dialect = new AnsiDialect();
        private final Placeholders placeholders = new Placeholders() {
            @Override
            public String marker() {
                return "?";
            } // fallback

            @Override
            public boolean supportsNamed() {
                return true;
            }

            @Override
            public String named(String name) {
                return ":" + name;
            }
        };

        @Override
        public String name() {
            return dialect.name();
        }

        @Override
        public IdentifierQuoter quoter() {
            return dialect.quoter();
        }

        @Override
        public ValueFormatter formatter() {
            return dialect.formatter();
        }

        @Override
        public Placeholders placeholders() {
            return placeholders;
        }

        @Override
        public Operators operators() {
            return dialect.operators();
        }

        @Override
        public Booleans booleans() {
            return dialect.booleans();
        }

        @Override
        public NullSorting nullSorting() {
            return dialect.nullSorting();
        }

        @Override
        public PaginationStyle paginationStyle() {
            return dialect.paginationStyle();
        }

        @Override
        public RenderersRepository renderers() {
            return dialect.renderers();
        }
    }
}
