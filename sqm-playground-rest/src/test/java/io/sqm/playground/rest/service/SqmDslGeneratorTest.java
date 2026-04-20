package io.sqm.playground.rest.service;

import io.sqm.core.Query;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.playground.api.SqlDialectDto;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqmDslGeneratorTest {

    @Test
    void toDslGeneratesJavaSourceForStatementAndNamedParams() {
        var statement = select(star())
            .from(tbl("customer"))
            .where(param("status").isNotNull())
            .build();

        var source = new SqmDslGenerator().toDsl(statement, SqlDialectDto.ansi);

        assertTrue(source.contains("package sqm.codegen;"));
        assertTrue(source.contains("public final class MyStatement"));
        assertTrue(source.contains("public static SelectQuery getStatement()"));
        assertTrue(source.contains("param(\"status\")"));
        assertTrue(source.contains("public static Set<String> getStatementParams()"));
        assertTrue(source.contains("return Set.of(\"status\")"));
    }

    @Test
    void toDslFormatsUpdateStatementsAcrossMultipleLines() {
        var statement = update(tbl("orders").as("o"))
            .joins(inner(tbl("customer").as("c")).on(col("c", "id").eq(col("o", "customer_id"))))
            .set("o", "status", lit("priority"))
            .where(col("c", "vip").eq(lit(1L)))
            .build();

        var source = new SqmDslGenerator().toDsl(statement, SqlDialectDto.ansi);

        assertTrue(source.contains("return update("));
        assertTrue(source.contains(".joins("));
        assertTrue(source.contains(".set("));
        assertTrue(source.contains(".where("));
        assertTrue(source.contains(".build();"));
    }

    @Test
    void toDslGeneratesJavaSourceForWithQueryStatements() {
        var parseContext = ParseContext.of(new AnsiSpecs());
        var sql = """
            with regional_sales as (
                select
                    c.region,
                    o.customer_id,
                    sum(o.total) as revenue
                from orders o
                join customer c on c.id = o.customer_id
                group by c.region, o.customer_id
            )
            select region, customer_id, revenue
            from regional_sales
            where revenue > 1000
            order by revenue desc
            """;
        var statement = parseContext.parse(Query.class, sql).value();

        var source = new SqmDslGenerator().toDsl(statement, SqlDialectDto.ansi);

        assertTrue(source.contains("return with("));
        assertTrue(source.contains("cte(\"regional_sales\","));
        assertTrue(source.contains(".body("));
        assertTrue(source.contains("tbl(\"regional_sales\")"));
    }
}
