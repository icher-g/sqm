package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.WithQuery;
import io.cherlabs.sqm.render.DefaultSqlWriter;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.ansi.spi.AnsiDialect;
import io.cherlabs.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.cherlabs.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class WithQueryRendererTest {

    /* ===================== HAPPY PATHS ===================== */

    private static String renderWith(WithQuery with) {
        RenderContext ctx = ctx();
        SqlWriter w = new DefaultSqlWriter(ctx);
        w.append(with);
        return w.toText(List.of()).sql();
    }

    /* ===================== ERROR CASES ===================== */

    private static RenderContext ctx() {
        // Replace if your context factory is different
        return RenderContext.of(new AnsiDialect());
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    /* ===================== HELPERS ===================== */

    private static void assertContainsInOrder(String sql, String... tokens) {
        String norm = normalize(sql);
        int pos = 0;
        for (String t : tokens) {
            int i = norm.indexOf(t, pos);
            if (i < 0) {
                fail("Missing token '" + t + "' in order. SQL:\n" + norm);
            }
            pos = i + t.length();
        }
    }

    @Test
    @DisplayName("WITH with mixed CTEs: CteQuery (with aliases) + CteQuery without aliases; outer query in WithQuery body")
    void with_mixedCtes_outerBody_ok() {
        // CTE #1: CteQuery with aliases
        var cteUsers = cte("cte_users")
            .columnAliases("id", "name")
            .select(query()
                .select(col("u", "id"), col("u", "name"))
                .from(tbl("users").as("u"))
            );

        // CTE #2: plain Query used as CTE (no column aliases)
        var cteOrders = cte("cte_orders")
            .select(query()
                .select(col("o", "user_id"), col("o", "amount"))
                .from(tbl("orders").as("o"))
            );

        // WITH ... (cte_users, cte_orders)  <— CTEs live in WithQuery.getQueries()
        // Outer body is the WithQuery itself (inherited Query fields)
        var q = with(cteUsers, cteOrders)
            .select(query()
                .select(col("x", "id"))
                .from(tbl("cte_users").as("x"))
            );

        var sql = renderWith(q);

        // Loose, order-preserving checks on the normalized SQL:
        assertContainsInOrder(sql,
            "WITH",
            "cte_users", "(", "id", ",", "name", ")", "AS", "(",
            "SELECT", "u.id", ",", "u.name", "FROM", "users", "AS", "u",
            ")", ",",
            "cte_orders", "AS", "(",
            "SELECT", "o.user_id", ",", "o.amount", "FROM", "orders", "AS", "o",
            ")",
            "SELECT", "x.id", "FROM", "cte_users", "AS", "x"
        );
    }

    @Test
    @DisplayName("WITH RECURSIVE is prefixed when flag set; outer body selects from the CTE")
    void with_recursive_prefix_ok() {
        var nums = cte("nums")
            .columnAliases("n")
            .select(query()
                .select(col("s", "n"))
                .from(tbl("seed_numbers").as("s"))
            );

        var q = with(nums)
            .recursive(true)
            .select(query()
                .select(col("t", "n"))
                .from(tbl("nums").as("t"))
            );

        var sql = normalize(renderWith(q));
        assertTrue(sql.startsWith("WITH RECURSIVE "), "Expected WITH RECURSIVE prefix");
        assertTrue(sql.contains("nums (n) AS ("), "Expected aliased CTE header");
        assertTrue(sql.contains("SELECT t.n FROM nums AS t"), "Expected outer query selecting from CTE");
    }

    @Test
    @DisplayName("WITH with only CteQuery CTEs (no plain Query CTEs)")
    void with_onlyCteQueries_ok() {
        var c1 = cte("c1")
            .select(query()
                .select(col("a", "id"))
                .from(tbl("users").as("a"))
            );

        var c2 = cte("c2")
            .select(query()
                .select(col("b", "user_id"))
                .from(tbl("orders").as("b"))
            );

        var q = with(c1, c2)
            .select(query()
                .select(col("b", "user_id")).from(tbl("c2").as("b"))
            );

        var sql = normalize(renderWith(q));
        assertTrue(sql.contains("WITH"), "Expected WITH");
        assertTrue(sql.contains("c1 AS ("), "First CTE should render");
        assertTrue(sql.contains("c2 AS ("), "Second CTE should render");
        assertTrue(sql.contains("SELECT b.user_id FROM c2 AS b"), "Outer body should select from c2");
    }

    @Test
    @DisplayName("Empty CTE list -> throws")
    void with_emptyCtes_throws() {
        var q = with()
            .select(query()
                .select(col("t", "id")).from(tbl("users", "t"))
            );
        var ex = assertThrows(IllegalArgumentException.class, () -> renderWith(q));
        assertTrue(ex.getMessage().toLowerCase().contains("with requires at least one query"));
    }

    @Test
    @DisplayName("CTE without name should lead to a clear failure")
    void cte_without_name_fails() {
        var nameless = cte(null)
            .select(query()
                .select(col("u", "id"))
                .from(tbl("users", "u"))
            );

        var q = with(nameless)
            .select(query()
                .select(col("u", "id"))
                .from(tbl("users", "u"))
            );

        // Depending on your writer/renderer, this may NPE or be validated elsewhere.
        // We just assert that rendering fails with some exception.
        assertThrows(RuntimeException.class, () -> renderWith(q));
    }
}
