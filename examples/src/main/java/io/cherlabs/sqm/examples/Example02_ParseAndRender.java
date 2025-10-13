package io.cherlabs.sqm.examples;

import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.parser.QueryParser;
import io.cherlabs.sqm.render.ansi.spi.AnsiDialect;
import io.cherlabs.sqm.render.spi.RenderContext;

public final class Example02_ParseAndRender {
    public static void main(String[] args) {
        String sql = """
            SELECT u.user_name, o.status, COUNT(*) AS cnt
            FROM orders AS o
            JOIN users AS u ON u.id = o.user_id
            WHERE o.status IN ('A','B')
            GROUP BY u.user_name, o.status
            HAVING COUNT(*) > 10
            ORDER BY cnt DESC
            OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
            """;

        QueryParser p = new QueryParser();
        var result = p.parse(sql);

        if (result.isError()) {
            System.err.println("Parse error: " + result.errorMessage());
            System.exit(1);
        }

        Query q = result.value();

        var ctx = RenderContext.of(new AnsiDialect());
        var sqlText = ctx.render(q);

        System.out.println("=== Canonical ANSI SQL ===");
        System.out.println(sqlText.sql());
    }
}
