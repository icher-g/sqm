package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.render.ansi.spi.AnsiDialect;
import io.cherlabs.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.cherlabs.sqm.core.CompositeQuery.Op.*;
import static org.junit.jupiter.api.Assertions.*;

class CompositeQueryRendererTest {

    /* ===== helpers ===== */

    private static SelectQuery sel(String table, String alias, String... cols) {
        var q = new SelectQuery();
        for (String c : cols) q.select(Column.of(c).from(alias));
        return q.from(Table.of(table).as(alias));
    }

    private static String render(CompositeQuery cq) {
        RenderContext ctx = RenderContext.of(new AnsiDialect());
        return ctx.render(cq).sql().replaceAll("\\s+", " ").trim();
    }

    /* ===== tests ===== */

    @Test
    @DisplayName("UNION ALL (2 terms) + final ORDER + FETCH FIRST")
    void unionAll_twoTerms_withFinalOrderAndFetch() {
        var t1 = sel("users", "u", "id", "name");
        var t2 = sel("archived_users", "a", "id", "name");

        var cq = new CompositeQuery(List.of(t1, t2), List.of(unionAll()))
            .orderBy(Order.by(Column.of("id").from("1")).asc()) // assuming ordinal or expr handled by your Order renderer
            .limit(10L);

        var sql = render(cq);

        assertTrue(sql.contains("( SELECT u.id, u.name FROM users AS u ) UNION ALL ( SELECT a.id, a.name FROM archived_users AS a )"),
            "Expected parenthesized terms with UNION ALL");
        assertTrue(sql.contains("ORDER BY"), "Expected ORDER BY at the end");
        assertTrue(sql.endsWith("FETCH NEXT 10 ROWS ONLY"), "Expected ANSI FETCH NEXT");
    }

    @Test
    @DisplayName("UNION then INTERSECT (3 terms) + final OFFSET/FETCH")
    void union_then_intersect_threeTerms_withOffsetFetch() {
        var t1 = sel("t1", "t1", "id");
        var t2 = sel("t2", "t2", "id");
        var t3 = sel("t3", "t3", "id");

        var cq = new CompositeQuery(
            List.of(t1, t2, t3),
            List.of(union(), intersect())
        ).offset(20L).limit(5L);

        var sql = render(cq);

        assertTrue(sql.contains(") UNION ("),
            "Expected UNION between first two terms");
        assertTrue(sql.contains(") INTERSECT ("),
            "Expected INTERSECT between second and third terms");
        assertTrue(sql.contains("OFFSET 20 ROWS"), "Expected final OFFSET");
        assertTrue(sql.endsWith("FETCH NEXT 5 ROWS ONLY"), "Expected FETCH NEXT with offset present");
    }

    @Test
    @DisplayName("EXCEPT ALL (2 terms) without final ORDER/FETCH")
    void exceptAll_twoTerms_noOrderFetch() {
        var left = sel("a", "a", "k");
        var right = sel("b", "b", "k");

        var cq = new CompositeQuery(List.of(left, right), List.of(exceptAll()));

        var sql = render(cq);
        assertTrue(sql.contains(") EXCEPT ALL ("), "Expected EXCEPT ALL");
        assertFalse(sql.contains("ORDER BY"), "No final ORDER BY expected");
        assertFalse(sql.contains("FETCH"), "No FETCH expected");
    }

    @Test
    @DisplayName("Term with ORDER BY -> throws (ANSI forbids per-term ORDER)")
    void termWithOrderBy_throws() {
        var t1 = sel("users", "u", "id");
        var t2 = sel("users2", "u2", "id").orderBy(Order.by(Column.of("id").from("u2")).asc());

        var cq = new CompositeQuery(List.of(t1, t2), List.of(union()));

        var ex = assertThrows(UnsupportedOperationException.class,
            () -> render(cq));
        assertTrue(ex.getMessage().toLowerCase().contains("operands"));
        assertTrue(ex.getMessage().toLowerCase().contains("order"));
    }

    @Test
    @DisplayName("Bad constructor sizes -> throws")
    void badConstructorSizes_throws() {
        var t1 = sel("t1", "t1", "id");
        var ex = assertThrows(IllegalArgumentException.class,
            () -> new CompositeQuery(List.of(t1), List.of(union())));
        assertTrue(ex.getMessage().contains("terms.size()"));
    }
}
