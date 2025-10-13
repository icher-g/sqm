package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.CteQuery;
import io.cherlabs.sqm.core.Table;
import io.cherlabs.sqm.render.DefaultSqlWriter;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.ansi.spi.AnsiDialect;
import io.cherlabs.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.cherlabs.sqm.dsl.Dsl.query;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CteQueryRendererTest {

    private static Column col(String alias, String name) {
        return Column.of(name).from(alias);
    }

    private static Table tbl(String name, String alias) {
        return Table.of(name).as(alias);
    }

    private static RenderContext ctx() {
        // Replace if your context factory is different
        return RenderContext.of(new AnsiDialect());
    }

    /* ===================== HELPERS ===================== */

    private static String render(CteQuery cte) {
        RenderContext ctx = ctx();
        SqlWriter w = new DefaultSqlWriter(ctx);
        w.append(cte);
        return w.toText(List.of()).sql();
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("CTE with aliases renders: name (a, b) AS ( <inner-select> )")
    void cte_with_aliases_ok() {
        var q = query()
            .select(col("u", "id"), col("u", "name"))
            .from(tbl("users", "u"));
        var cte = new CteQuery("u_cte", q, List.of("id", "name"));
        var sql = render(cte);
        var norm = normalize(sql);
        assertTrue(norm.contains("u_cte (id, name) AS ("), "Header with aliases expected");
        assertTrue(norm.contains("SELECT u.id, u.name FROM users AS u"), "Inner SELECT expected");
        assertTrue(norm.trim().endsWith(")"), "CTE should close with ')'");
    }

    @Test
    @DisplayName("CTE without aliases renders: name AS ( <inner-select> )")
    void cte_without_aliases_ok() {
        var q = query()
            .select(col("o", "user_id"), col("o", "amount"))
            .from(tbl("orders", "o"));
        var cte = new CteQuery("orders_cte", q, null);
        var sql = render(cte);

        var norm = normalize(sql);
        assertTrue(norm.startsWith("orders_cte AS ("), "Header without aliases expected");
        assertTrue(norm.contains("SELECT o.user_id, o.amount FROM orders AS o"), "Inner SELECT expected");
        assertTrue(norm.endsWith(")"), "CTE should close with ')'");
    }

    @Test
    @DisplayName("CTE missing name -> expect a render failure")
    void cte_missing_name_fails() {
        var q = query()
            .select(col("u", "id"))
            .from(tbl("users", "u"));
        var cte = new CteQuery(null, q, null);

        assertThrows(RuntimeException.class, () -> render(cte));
    }
}
