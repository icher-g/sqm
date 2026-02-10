package io.sqm.examples;

import io.sqm.core.Query;
import io.sqm.examples.generated.UserQueries;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;

/**
 * Demonstrates usage of queries generated from SQL files under {@code src/main/sql}.
 * by {@code sqm-codegen-maven-plugin}.
 */
public final class Query_UseGeneratedSqlFiles {

    private Query_UseGeneratedSqlFiles() {
    }

    /**
     * Builds SQM models from generated methods and renders SQL text.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        Query findById = UserQueries.findById();
        Query listActive = UserQueries.listActive();

        var ctx = RenderContext.of(new AnsiDialect());
        var sql1 = ctx.render(findById);
        var sql2 = ctx.render(listActive);

        System.out.println("=== Generated Query: findById ===");
        System.out.println(sql1.sql());
        System.out.println("Parameters: " + UserQueries.findByIdParams());

        System.out.println("=== Generated Query: listActive ===");
        System.out.println(sql2.sql());
        System.out.println("Parameters: " + UserQueries.listActiveParams());
    }
}
