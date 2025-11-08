package io.sqm.examples;

import io.sqm.core.Query;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;

import static io.sqm.dsl.Dsl.*;

public final class Query_BuildSimpleQueryWithDSL {
    public static void main(String[] args) {
        Query q = select(
            col("u", "user_name"),
            col("o", "status"),
            func("count", starArg()).as("cnt")
        )
        .from(tbl("orders").as("o"))
        .join(
            inner(tbl("users").as("u"))
                .on(col("u", "id").eq(col("o", "user_id")))
        )
        .where(col("o", "status").in("A", "B"))
        .groupBy(group("u", "user_name"), group("o", "status"))
        .having(func("count", starArg()).gt(10))
        .orderBy(order("cnt"))
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
