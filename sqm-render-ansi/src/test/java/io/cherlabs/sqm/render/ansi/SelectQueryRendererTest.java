package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.SelectQuery;
import io.cherlabs.sqm.render.DefaultSqlWriter;
import io.cherlabs.sqm.render.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.DefaultRenderContext;
import io.cherlabs.sqm.render.ansi.spi.AnsiDialect;
import io.cherlabs.sqm.render.spi.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.cherlabs.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AnsiQueryRenderer focusing on pagination branches.
 * Uses actual model entities + BufferSqlWriter; no mocks.
 */
public class SelectQueryRendererTest {

    // -----------------------
    // Test plumbing / helpers
    // -----------------------

    private static String toSql(Renderer<SelectQuery> r, SelectQuery q, RenderContext ctx) {
        SqlWriter w = new DefaultSqlWriter(ctx);
        r.render(q, ctx, w);
        return w.toText(List.of()).sql(); // your SqlWriter usually returns SqlText; adapt if needed
    }

    private static PaginationStyle limitOffset() {
        return new PaginationStyle() {
            @Override
            public boolean supportsLimitOffset() {
                return true;
            }

            @Override
            public boolean supportsOffsetFetch() {
                return false;
            }

            @Override
            public boolean supportsTop() {
                return false;
            }
        };
    }

    private static PaginationStyle offsetFetch() {
        return new PaginationStyle() {
            @Override
            public boolean supportsLimitOffset() {
                return false;
            }

            @Override
            public boolean supportsOffsetFetch() {
                return true;
            }

            @Override
            public boolean supportsTop() {
                return false;
            }
        };
    }

    private static PaginationStyle topOnly() {
        return new PaginationStyle() {
            @Override
            public boolean supportsLimitOffset() {
                return false;
            }

            @Override
            public boolean supportsOffsetFetch() {
                return false;
            }

            @Override
            public boolean supportsTop() {
                return true;
            }
        };
    }

    private RenderContext ctxWith(PaginationStyle style) {
        return new RenderContext() {
            private final DefaultRenderContext ctx = new DefaultRenderContext(new AnsiDialect());
            private final SqlDialect dialect = ctx.dialect();

            @Override
            public SqlDialect dialect() {
                return new SqlDialect() {
                    @Override
                    public String name() {
                        return "";
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
                        return dialect.placeholders();
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
                        return style;
                    }

                    @Override
                    public RenderersRepository renderers() {
                        return dialect.renderers();
                    }
                };
            }

            @Override
            public ParamSink params() {
                return ctx.params();
            }
        };
    }

    // -----------------------
    // Tests
    // -----------------------

    @Nested
    @DisplayName("LIMIT / OFFSET style")
    class LimitOffsetStyle {

        @Test
        @DisplayName("LIMIT + OFFSET -> tail 'LIMIT n OFFSET m'")
        void limitAndOffset() {
            var ctx = ctxWith(limitOffset());
            var r = new SelectQueryRenderer();

            var q = query()
                .select(col("t1", "c1"))
                .from(tbl("t1"))
                .orderBy(asc(col("t1", "c1")))
                .limit(10L)
                .offset(5L);

            String sql = toSql(r, q, ctx);

            assertTrue(sql.contains("LIMIT 10"), "should contain LIMIT 10");
            assertTrue(sql.contains("OFFSET 5"), "should contain OFFSET 5");
            assertTrue(sql.indexOf("LIMIT 10") < sql.indexOf("OFFSET 5"), "LIMIT should appear before OFFSET for ANSI/MySQL/PG style");
        }

        @Test
        @DisplayName("LIMIT only -> tail 'LIMIT n'")
        void limitOnly() {
            var ctx = ctxWith(limitOffset());
            var r = new SelectQueryRenderer();

            var q = query()
                .select(col("t", "c"))
                .from(tbl("t"))
                .limit(3L);

            String sql = toSql(r, q, ctx);
            assertTrue(sql.endsWith("LIMIT 3") || sql.contains("\nLIMIT 3"),
                "expected LIMIT 3 tail");
        }

        @Test
        @DisplayName("OFFSET only -> tail 'OFFSET m'")
        void offsetOnly() {
            var ctx = ctxWith(limitOffset());
            var r = new SelectQueryRenderer();

            var q = query()
                .select(col("t", "c"))
                .from(tbl("t"))
                .offset(7L);

            String sql = toSql(r, q, ctx);
            assertTrue(sql.endsWith("OFFSET 7") || sql.contains("\nOFFSET 7"),
                "expected OFFSET 7 tail");
        }
    }

    @Nested
    @DisplayName("OFFSET … FETCH style")
    class OffsetFetchStyle {
        @Test
        @DisplayName("ORDER BY present -> 'ORDER BY … OFFSET m ROWS FETCH NEXT n ROWS ONLY'")
        void offsetFetch_ok() {
            var ctx = ctxWith(offsetFetch());
            var r = new SelectQueryRenderer();

            var q = query()
                .select(col("t1", "c1"))
                .from(tbl("t1"))
                .orderBy(asc(col("t1", "c1")))
                .limit(10L)
                .offset(5L);

            String sql = toSql(r, q, ctx);
            assertTrue(sql.contains("ORDER BY"), "must include ORDER BY");
            assertTrue(sql.contains("OFFSET 5 ROWS"), "must include OFFSET 5 ROWS");
            assertTrue(sql.contains("FETCH NEXT 10 ROWS ONLY"), "must include FETCH NEXT 10 ROWS ONLY");
            assertTrue(sql.indexOf("ORDER BY") < sql.indexOf("OFFSET"), "ORDER BY must precede OFFSET … FETCH");
        }

        @Test
        @DisplayName("Only OFFSET -> 'ORDER BY … OFFSET m ROWS'")
        void onlyOffset_ok() {
            var ctx = ctxWith(offsetFetch());
            var r = new SelectQueryRenderer();

            var q = query()
                .select(col("t", "c"))
                .from(tbl("t"))
                .orderBy(asc(col("t", "c")))
                .offset(12L);

            String sql = toSql(r, q, ctx);
            assertTrue(sql.contains("ORDER BY"), "must include ORDER BY");
            assertTrue(sql.contains("OFFSET 12 ROWS"), "must include OFFSET 12 ROWS");
            assertFalse(sql.contains("FETCH NEXT"), "should not include FETCH NEXT when limit is absent");
        }
    }

    @Nested
    @DisplayName("TOP style")
    class TopStyle {

        @Test
        @DisplayName("LIMIT -> 'SELECT TOP n …' injected in head")
        void top_in_head() {
            var ctx = ctxWith(topOnly());
            var r = new SelectQueryRenderer();

            var q = query()
                .select(col("t", "c"))
                .from(tbl("t"))
                .limit(4L);

            String sql = toSql(r, q, ctx);
            // Allow both "SELECT TOP 4" and "SELECT DISTINCT TOP 4" if user sets distinct elsewhere.
            assertTrue(sql.startsWith("SELECT TOP 4") || sql.startsWith("SELECT DISTINCT TOP 4"), "TOP should be injected right after SELECT[/DISTINCT]");
            assertFalse(sql.contains("OFFSET"), "TOP style should not produce OFFSET");
        }

        @Test
        @DisplayName("OFFSET with TOP style -> throws UnsupportedOperationException")
        void top_with_offset_throws() {
            var ctx = ctxWith(topOnly());
            var r = new SelectQueryRenderer();

            var q = query()
                .select(col("t", "c"))
                .from(tbl("t"))
                .limit(4L)
                .offset(2L);

            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> toSql(r, q, ctx));
            assertTrue(ex.getMessage().toLowerCase().contains("offset"), "message should mention OFFSET not supported");
        }
    }
}
