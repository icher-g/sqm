package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WindowParsingTest {

    /* =================== Helpers =================== */

    private static OverSpec overFromSelectItem(SelectItem si) {
        var expr = ((ExprSelectItem) si).expr();
        var fn = (FunctionExpr) expr;
        return fn.over();
    }

    private SelectQuery parseSelect(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        var res = ctx.parse(SelectQuery.class, sql);
        if (res.isError()) {
            var p = res.problems().getFirst();
            throw new ParserException(p.message(), p.pos());
        }
        return res.value();
    }

    /* =================== Positive cases =================== */

    @Test
    void namedWindow_ref_in_over() {
        // SQL
        var sql = """
            SELECT emp_name, RANK() OVER w AS r
            FROM employees
            WINDOW w AS (PARTITION BY dept ORDER BY salary DESC)
            """;

        var q = parseSelect(sql);

        // Shape: WINDOW clause exists with a single WindowDef named "w"
        List<WindowDef> windows = q.windows();
        assertEquals(1, windows.size());
        assertEquals("w", windows.getFirst().name());

        // The RANK() uses OVER w (reference)
        var over = overFromSelectItem(q.items().get(1));
        assertInstanceOf(OverSpec.Ref.class, over);
        assertEquals("w", ((OverSpec.Ref) over).windowName());
    }

    @Test
    void over_inline_rows_single_bound_preceding() {
        var sql = """
            SELECT acct_id, ts,
                   SUM(amount) OVER (PARTITION BY acct_id ORDER BY ts ROWS 5 PRECEDING) AS s
            FROM tx
            """;

        var q = parseSelect(sql);

        var over = overFromSelectItem(q.items().get(2));
        assertInstanceOf(OverSpec.Def.class, over);
        var def = (OverSpec.Def) over;

        // PARTITION BY present
        assertNotNull(def.partitionBy());
        assertFalse(def.partitionBy().items().isEmpty());

        // ORDER BY present
        assertNotNull(def.orderBy());

        // Frame = Single (ROWS 5 PRECEDING)
        var frame = def.frame();
        assertInstanceOf(FrameSpec.Single.class, frame);
        var single = (FrameSpec.Single) frame;
        assertEquals(FrameSpec.Unit.ROWS, single.unit());
        assertInstanceOf(BoundSpec.Preceding.class, single.bound());
    }

    @Test
    void over_inline_groups_between_with_exclude_ties() {
        var sql = """
            SELECT grp, score,
                   RANK() OVER (PARTITION BY grp ORDER BY score DESC GROUPS BETWEEN 1 PRECEDING AND 1 FOLLOWING EXCLUDE TIES) AS rk
            FROM scores
            """;

        var q = parseSelect(sql);

        var over = overFromSelectItem(q.items().get(2));
        var def = (OverSpec.Def) over;

        // Frame = Between (GROUPS BETWEEN 1 PRECEDING AND 1 FOLLOWING)
        var frame = def.frame();
        assertInstanceOf(FrameSpec.Between.class, frame);
        var between = (FrameSpec.Between) frame;
        assertEquals(FrameSpec.Unit.GROUPS, between.unit());
        assertInstanceOf(BoundSpec.Preceding.class, between.start());
        assertInstanceOf(BoundSpec.Following.class, between.end());

        // EXCLUDE TIES
        assertNotNull(def.exclude());
        assertEquals(OverSpec.Exclude.TIES, def.exclude());
    }

    @Test
    void extend_base_window_with_frame_between() {
        var sql = """
            SELECT dept, emp_name,
                   SUM(salary) OVER (w ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS run_sum
            FROM employees
            WINDOW w AS (PARTITION BY dept ORDER BY salary DESC)
            """;

        var q = parseSelect(sql);

        // WindowDef "w" exists
        assertEquals(1, q.windows().size());
        assertEquals("w", q.windows().getFirst().name());

        var over = overFromSelectItem(q.items().get(2));
        var def = (OverSpec.Def) over;

        // Base name present (extending w)
        assertNotNull(def.baseWindow());
        assertEquals("w", def.baseWindow());

        // Frame = Between (UNBOUNDED PRECEDING .. CURRENT ROW)
        var between = (FrameSpec.Between) def.frame();
        assertInstanceOf(BoundSpec.UnboundedPreceding.class, between.start());
        assertInstanceOf(BoundSpec.CurrentRow.class, between.end());
    }

    @Test
    void multiple_named_windows_and_mixed_over_usage() {
        var sql = """
            SELECT x,
                   ROW_NUMBER() OVER w1 AS rn,
                   AVG(v) OVER w2 AS avg_all
            FROM t
            WINDOW w1 AS (PARTITION BY k ORDER BY ts),
                   w2 AS (ORDER BY v)
            """;

        var q = parseSelect(sql);
        assertEquals(2, q.windows().size());
        assertEquals("w1", q.windows().get(0).name());
        assertEquals("w2", q.windows().get(1).name());

        var over1 = overFromSelectItem(q.items().get(1));
        var over2 = overFromSelectItem(q.items().get(2));
        assertInstanceOf(OverSpec.Ref.class, over1);
        assertInstanceOf(OverSpec.Ref.class, over2);
    }

    @Test
    void aggregate_with_filter_and_over_partition_by() {
        var sql = """
            SELECT dept,
                   COUNT(DISTINCT user_id) FILTER (WHERE active) OVER (PARTITION BY dept) AS active_users
            FROM users
            """;

        var q = parseSelect(sql);

        var si = (ExprSelectItem) q.items().get(1);
        var fn = (FunctionExpr) si.expr();

        // DISTINCT + FILTER recognized by parser (not window-specific, but part of this shape)
        assertTrue(fn.distinctArg());
        assertNotNull(fn.filter());

        var over = fn.over();
        assertInstanceOf(OverSpec.Def.class, over);
        var def = (OverSpec.Def) over;
        assertNotNull(def.partitionBy());
        assertNull(def.orderBy());   // none in this example
        assertNull(def.frame());
    }

    @Test
    void window_clause_renders_before_select_order_by() {
        var sql = """
            SELECT val, RANK() OVER w AS rk
            FROM t
            WINDOW w AS (ORDER BY val)
            ORDER BY val
            """;

        var q = parseSelect(sql);

        // Sanity: has a WINDOW and a top-level ORDER BY
        assertEquals(1, q.windows().size());
        assertNotNull(q.orderBy());
    }

    /* =================== Negative basics (syntax) =================== */

    @Test
    void fail_missing_parens_in_over_def() {
        // Missing parentheses after OVER (must be OVER (...) or OVER <name>)
        var bad = "SELECT SUM(x) OVER PARTITION BY dept FROM t";
        assertThrows(ParserException.class, () -> parseSelect(bad));
    }

    @Test
    void fail_bad_window_def_missing_parens() {
        // WINDOW w AS PARTITION BY dept  -- must be AS (<spec>)
        var bad = """
            SELECT x FROM t
            WINDOW w AS PARTITION BY dept
            """;
        assertThrows(ParserException.class, () -> parseSelect(bad));
    }
}
