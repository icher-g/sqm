package io.sqm.render.ansi;

import io.sqm.core.Query;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WindowRenderingTest {

    private String render(Query q) {
        var ctx = RenderContext.of(new AnsiDialect());
        return ctx.render(q).sql().trim();
    }

    @Test
    void namedWindow_ref_in_over() {
        // SELECT emp_name, RANK() OVER w AS r
        // FROM employees
        // WINDOW w AS (PARTITION BY dept ORDER BY salary DESC)
        var q = select(
            col("emp_name"),
            func("RANK").over("w").as("r")
        )
            .from(tbl("employees"))
            .window(
                window("w",
                    partition(col("dept")),
                    orderBy(order(col("salary")).desc()))
            )
            .build();

        var sql = render(q);
        assertEquals("""
            SELECT emp_name, RANK() OVER w AS r
            FROM employees
            WINDOW w AS (PARTITION BY dept ORDER BY salary DESC)
            """.trim(), sql);
    }

    @Test
    void over_inline_rows_preceding() {
        // SELECT acct_id, ts, SUM(amount) OVER (PARTITION BY acct_id ORDER BY ts ASC ROWS 5 PRECEDING) AS s
        // FROM tx
        var q = select(
            col("acct_id"),
            col("ts"),
            func("SUM", arg(col("amount")))
                .over(
                    partition(col("acct_id")),
                    orderBy(order(col("ts")).asc()),
                    rows(preceding(5))
                ).as("s")
        )
            .from(tbl("tx"))
            .build();

        var sql = render(q);
        assertEquals("""
            SELECT acct_id, ts, SUM(amount) OVER (PARTITION BY acct_id ORDER BY ts ASC ROWS 5 PRECEDING) AS s
            FROM tx
            """.trim(), sql);
    }

    @Test
    void over_inline_between_with_exclude_ties() {
        // SELECT grp, score, RANK() OVER (PARTITION BY grp ORDER BY score DESC GROUPS BETWEEN 1 PRECEDING AND 1 FOLLOWING EXCLUDE TIES) AS rk
        // FROM scores
        var q = select(
            col("grp"),
            col("score"),
            func("RANK")
                .over(
                    partition(col("grp")),
                    orderBy(order(col("score")).desc()),
                    groups(preceding(1), following(1)),
                    excludeTies()
                ).as("rk")
        )
            .from(tbl("scores"))
            .build();

        var sql = render(q);
        assertEquals("""
            SELECT grp, score, RANK() OVER (PARTITION BY grp ORDER BY score DESC GROUPS BETWEEN 1 PRECEDING AND 1 FOLLOWING EXCLUDE TIES) AS rk
            FROM scores
            """.trim(), sql);
    }

    @Test
    void extend_base_window_with_frame() {
        // SELECT dept, emp_name, SUM(salary) OVER (w ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS run_sum
        // FROM employees
        // WINDOW w AS (PARTITION BY dept ORDER BY salary DESC)
        var q = select(
            col("dept"),
            col("emp_name"),
            func("SUM", arg(col("salary")))
                .over("w", rows(unboundedPreceding(), currentRow())
            ).as("run_sum")
        )
            .from(tbl("employees"))
            .window(
                window("w",
                    partition(col("dept")),
                    orderBy(order(col("salary")).desc())
                )
            )
            .build();

        var sql = render(q);
        assertEquals("""
            SELECT dept, emp_name, SUM(salary) OVER (w ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS run_sum
            FROM employees
            WINDOW w AS (PARTITION BY dept ORDER BY salary DESC)
            """.trim(), sql);
    }

    @Test
    void multiple_named_windows_and_mixed_usage() {
        // SELECT x, ROW_NUMBER() OVER w1 AS rn, AVG(v) OVER w2 AS avg_all
        // FROM t
        // WINDOW
        //   w1 AS (PARTITION BY k ORDER BY ts),
        //   w2 AS (ORDER BY v)
        var q = select(
            col("x"),
            func("ROW_NUMBER").over("w1").as("rn"),
            func("AVG", arg(col("v"))).over("w2").as("avg_all")
        )
            .from(tbl("t"))
            .window(
                window("w1", partition(col("k")), orderBy(order(col("ts")).asc())),
                window("w2", partition(), orderBy(order(col("v")).asc())) // partition() with 0 args -> no PARTITION BY
            )
            .build();

        var sql = render(q);
        assertEquals("""
            SELECT x, ROW_NUMBER() OVER w1 AS rn, AVG(v) OVER w2 AS avg_all
            FROM t
            WINDOW w1 AS (PARTITION BY k ORDER BY ts ASC)
            WINDOW w2 AS (ORDER BY v ASC)
            """.trim(), sql);
    }

    @Test
    void aggregate_with_filter_and_over() {
        // SELECT dept, COUNT(DISTINCT user_id) FILTER (WHERE active) OVER (PARTITION BY dept) AS active_users
        // FROM users
        var countDistinctActive =
            func("COUNT", arg(col("user_id")))
                .distinct()
                .filter(col("active").eq(lit(true)))
                .over(over(partition(col("dept"))));

        var q = select(
            col("dept"),
            countDistinctActive.as("active_users")
        )
            .from(tbl("users"))
            .build();

        var sql = render(q);
        assertEquals("""
            SELECT dept, COUNT(DISTINCT user_id) FILTER (WHERE active = TRUE) OVER (PARTITION BY dept) AS active_users
            FROM users
            """.trim(), sql);
    }

    @Test
    void window_clause_appears_before_order_by() {
        // Ensure WINDOW is rendered before SELECT-level ORDER BY.
        // SELECT val, RANK() OVER w AS rk
        // FROM t
        // WINDOW w AS (ORDER BY val)
        // ORDER BY val
        var q = select(
            col("val"),
            func("RANK").over("w").as("rk")
        )
            .from(tbl("t"))
            .window(window("w", partition(), orderBy(order(col("val")).asc())))
            .orderBy(order(col("val")).asc())
            .build();

        var sql = render(q);
        assertEquals("""
            SELECT val, RANK() OVER w AS rk
            FROM t
            WINDOW w AS (ORDER BY val ASC)
            ORDER BY val ASC
            """.trim(), sql);
    }
}

