package io.sqm.playground.rest.service;

import io.sqm.playground.api.SqlDialectDto;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.param;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.star;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static io.sqm.dsl.Dsl.col;
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
        assertTrue(source.contains("public final class MyQuery"));
        assertTrue(source.contains("public static SelectQuery getStatement()"));
        assertTrue(source.contains("param(\"status\")"));
        assertTrue(source.contains("public static Set<String> getStatementParams()"));
        assertTrue(source.contains("return Set.of(\"status\")"));
    }

    @Test
    void toDslFormatsUpdateStatementsAcrossMultipleLines() {
        var statement = update(tbl("orders").as("o"))
            .joins(inner(tbl("customer").as("c")).on(col("c", "id").eq(col("o", "customer_id"))))
            .set(set("o", "status", lit("priority")))
            .where(col("c", "vip").eq(lit(1L)))
            .build();

        var source = new SqmDslGenerator().toDsl(statement, SqlDialectDto.ansi);

        assertTrue(source.contains("return update(tbl(\"orders\").as(\"o\"))"));
        assertTrue(source.contains("\n        .joins("));
        assertTrue(source.contains("\n        .set("));
        assertTrue(source.contains("\n        .where("));
        assertTrue(source.contains("\n        .build();"));
    }
}
