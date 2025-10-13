package io.cherlabs.sqm.examples;

import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.render.ansi.spi.AnsiDialect;
import io.cherlabs.sqm.render.spi.RenderContext;

import static io.cherlabs.sqm.dsl.Dsl.*;

public final class Example01_SelectJoinGroup {
    public static void main(String[] args) {
        Query q = query()
            .select(
                col("u", "user_name"),
                col("o", "status"),
                func("count", star()).as("cnt")
            )
            .from(tbl("orders").as("o"))
            .join(
                inner(tbl("users").as("u"))
                    .on(col("u", "id").eq(col("o", "user_id")))
            )
            .where(col("o", "status").in("A", "B"))
            .groupBy(group("u", "user_name"), group("o", "status"))
            .having(func("count", star()).gt(10))
            .orderBy(desc(col("cnt")))
            .limit(10)
            .offset(20);

        var ctx = RenderContext.of(new AnsiDialect());
        var sql = ctx.render(q);

        System.out.println("=== ANSI SQL ===");
        System.out.println(sql.sql());

//        generated SQL:
//
//        SELECT u.user_name, o.status, count(*) AS cnt
//        FROM orders AS o
//        INNER JOIN users AS u ON u.id = o.user_id
//        WHERE o.status IN ('A', 'B')
//        GROUP BY u.user_name, o.status
//        HAVING count(*) > 10
//        ORDER BY cnt DESC
//        OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY  -- LIMIT and OFFSET format is defined by the
    }
}
