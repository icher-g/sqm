package io.sqm.render.ansi;

import io.sqm.core.CteDef;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CteDefRendererTest {

    private static RenderContext ctx() {
        // Replace if your context factory is different
        return RenderContext.of(new AnsiDialect());
    }

    /* ===================== HELPERS ===================== */

    private static String render(CteDef cte) {
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
        var q = select(col("u", "id"), col("u", "name"))
            .from(tbl("users").as("u"));
        var cte = cte("u_cte", q).columnAliases("id", "name");
        var sql = render(cte);
        var norm = normalize(sql);
        assertTrue(norm.contains("u_cte (id, name) AS ("), "Header with aliases expected");
        assertTrue(norm.contains("SELECT u.id, u.name FROM users AS u"), "Inner SELECT expected");
        assertTrue(norm.trim().endsWith(")"), "CTE should close with ')'");
    }

    @Test
    @DisplayName("CTE without aliases renders: name AS ( <inner-select> )")
    void cte_without_aliases_ok() {
        var q = select(col("o", "user_id"), col("o", "amount"))
            .from(tbl("orders").as("o"));
        var cte = cte("orders_cte", q);
        var sql = render(cte);

        var norm = normalize(sql);
        assertTrue(norm.startsWith("orders_cte AS ("), "Header without aliases expected");
        assertTrue(norm.contains("SELECT o.user_id, o.amount FROM orders AS o"), "Inner SELECT expected");
        assertTrue(norm.endsWith(")"), "CTE should close with ')'");
    }

    @Test
    @DisplayName("CTE missing name -> expect a render failure")
    void cte_missing_name_fails() {
        var q = select(col("u", "id"))
            .from(tbl("users", "u"));
        var cte = cte(null, q);

        assertThrows(RuntimeException.class, () -> render(cte));
    }
}
