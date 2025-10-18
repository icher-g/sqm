package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.Direction;
import io.cherlabs.sqm.core.Nulls;
import io.cherlabs.sqm.core.Order;
import io.cherlabs.sqm.render.*;
import io.cherlabs.sqm.render.ansi.statement.OrderRenderer;
import io.cherlabs.sqm.render.spi.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for OrderItemRenderer.
 * <p>
 * Notes:
 * - Uses a tiny in-test Column renderer to keep things self-contained.
 * - If your project already registers a column renderer in the dialect,
 * you can drop TestRenderers and wire the real one.
 */
class OrderRendererTest {

    private final Renderer<Order> renderer = new OrderRenderer();

    // ---------- Helpers ----------

    private static SqlDialect dialect(IdentifierQuoter quoter, NullSorting nulls) {
        return new SqlDialect() {
            @Override
            public String name() {
                return "";
            }

            @Override
            public IdentifierQuoter quoter() {
                return quoter;
            }

            @Override
            public DefaultValueFormatter formatter() {
                return null;
            }

            @Override
            public Placeholders placeholders() {
                return null;
            }

            @Override
            public Operators operators() {
                return null;
            }

            @Override
            public Booleans booleans() {
                return null;
            }

            @Override
            public NullSorting nullSorting() {
                return nulls;
            }

            @Override
            public PaginationStyle paginationStyle() {
                return null;
            }

            @Override
            public RenderersRepository renderers() {
                return Renderers.ansi();
            }
            // other SqlDialect members (operators, pagination, etc.) are not used here
        };
    }

    private static RenderContext ctx(SqlDialect d) {
        return new RenderContext() {
            @Override
            public SqlDialect dialect() {
                return d;
            }

            @Override
            public ParamSink params() {
                return null;
            }
        };
    }

    private static IdentifierQuoter passThruQuoter() {
        return new IdentifierQuoter() {
            @Override
            public String quote(String ident) {
                return "\"" + ident + "\"";
            }

            @Override
            public String quoteIfNeeded(String ident) {
                return ident;
            } // keep simple for assertions

            @Override
            public String qualify(String schemaOrNull, String name) {
                return "";
            }

            @Override
            public boolean needsQuoting(String identifier) {
                return false;
            }
        };
    }

    private static IdentifierQuoter quotingHyphenQuoter() {
        return new IdentifierQuoter() {
            @Override
            public String quote(String ident) {
                return "\"" + ident.replace("\"", "\"\"") + "\"";
            }

            @Override
            public String quoteIfNeeded(String ident) {
                return ident.indexOf('-') >= 0 ? quote(ident) : ident;
            }

            @Override
            public String qualify(String schemaOrNull, String name) {
                return "";
            }

            @Override
            public boolean needsQuoting(String identifier) {
                return false;
            }
        };
    }

    private static NullSorting explicitNulls() {
        return new NullSorting() {
            @Override
            public boolean supportsExplicit() {
                return true;
            }

            @Override
            public String keyword(Nulls n) {
                return switch (n) {
                    case First -> "NULLS FIRST";
                    case Last -> "NULLS LAST";
                    case Default -> ""; // won't be used directly; DEFAULT is mapped via defaultFor(...)
                };
            }

            @Override
            public Nulls defaultFor(Direction dir) {
                return dir == Direction.Desc ? Nulls.First : Nulls.Last; // typical
            }
        };
    }

    private static NullSorting noExplicitNulls() {
        return new NullSorting() {
            @Override
            public boolean supportsExplicit() {
                return false;
            }

            @Override
            public String keyword(Nulls n) {
                return "";
            }

            @Override
            public Nulls defaultFor(Direction dir) {
                return Nulls.Last;
            }
        };
    }

    private static String renderToSql(Renderer<Order> r, Order item, RenderContext rc) {
        SqlWriter w = new DefaultSqlWriter(rc);
        r.render(item, rc, w);
        return w.toText(List.of()).sql();
    }

    private static Column col(String t, String c) {
        return Column.of(c).from(t);
    }

    // ---------- Tests ----------

    @Test
    @DisplayName("Renders: expr COLLATE ... ASC NULLS FIRST (explicit-null dialect)")
    void collate_dir_nulls_order_explicit() {
        var d = dialect(passThruQuoter(), explicitNulls());
        var rc = ctx(d);

        var item = new Order(col("t", "c"), Direction.Asc, Nulls.First, "de_CH");
        String sql = renderToSql(renderer, item, rc);

        // must include pieces in the right order
        int iExpr = sql.indexOf("t.c");
        int iColl = sql.indexOf(" COLLATE de_CH");
        int iDir = sql.indexOf(" ASC");
        int iNull = sql.indexOf(" NULLS FIRST");

        assertTrue(iExpr >= 0, "should render column");
        assertTrue(iColl > iExpr, "COLLATE after expr");
        assertTrue(iDir > iColl, "ASC after COLLATE");
        assertTrue(iNull > iDir, "NULLS FIRST after direction");
    }

    @Test
    @DisplayName("Nulls DEFAULT maps via dialect defaultFor(direction)")
    void nulls_default_mapped_by_dialect() {
        var d = dialect(passThruQuoter(), explicitNulls());
        var rc = ctx(d);

        // DEFAULT + DESC -> dialect says FIRST
        var item = new Order(col("t", "c"), Direction.Desc, Nulls.Default, null);
        String sql = renderToSql(renderer, item, rc);

        assertTrue(sql.contains(" DESC"), "should render DESC");
        assertTrue(sql.contains(" NULLS FIRST"), "DEFAULT must map to FIRST for DESC per dialect");
    }

    @Test
    @DisplayName("Nulls DEFAULT with no direction -> treated as ASC for default mapping")
    void nulls_default_no_direction_treated_as_asc() {
        var d = dialect(passThruQuoter(), explicitNulls());
        var rc = ctx(d);

        // no direction -> renderer treats as ASC for default mapping -> LAST
        var item = new Order(col("t", "c"), null, Nulls.Default, null);
        String sql = renderToSql(renderer, item, rc);

        assertFalse(sql.contains(" ASC"), "direction unspecified -> no ASC printed");
        assertTrue(sql.contains(" NULLS LAST"), "DEFAULT without dir -> map as ASC -> LAST");
    }

    @Test
    @DisplayName("Dialect without explicit NULLS -> ignore nulls clause")
    void nulls_ignored_when_not_supported() {
        var d = dialect(passThruQuoter(), noExplicitNulls());
        var rc = ctx(d);

        var item = new Order(col("t", "c"), Direction.Asc, Nulls.First, null);
        String sql = renderToSql(renderer, item, rc);

        assertTrue(sql.contains(" ASC"), "should render ASC");
        assertFalse(sql.contains("NULLS"), "NULLS must be omitted when dialect doesn't support explicit keywords");
    }

    @Test
    @DisplayName("COLLATE uses quoter.quoteIfNeeded (e.g., hyphen -> quoted)")
    void collate_uses_quoter_quote_if_needed() {
        var d = dialect(quotingHyphenQuoter(), explicitNulls());
        var rc = ctx(d);

        var item = new Order(col("t", "c"), null, null, "de-CH");
        String sql = renderToSql(renderer, item, rc);

        assertTrue(sql.contains(" COLLATE \"de-CH\""), "collation with hyphen should be quoted by quoter");
        // no dir, no nulls
        assertFalse(sql.contains(" ASC") || sql.contains(" DESC"));
        assertFalse(sql.contains("NULLS"));
    }
}
