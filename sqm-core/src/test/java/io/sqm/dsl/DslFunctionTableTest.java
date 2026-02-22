package io.sqm.dsl;

import io.sqm.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DSL - FunctionTable and Lateral Tests")
class DslFunctionTableTest {

    @Test
    @DisplayName("tbl() with FunctionExpr")
    void tblWithFunctionExpr() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = tbl(func);

        assertNotNull(table);
        assertInstanceOf(FunctionTable.class, table);
        assertEquals(func, table.function());
    }

    @Test
    @DisplayName("tbl() creates function table with DSL")
    void tblCreatesFunctionTable() {
        var table = tbl(func("unnest", arg(array(lit(1), lit(2), lit(3)))));

        assertNotNull(table);
        assertInstanceOf(FunctionTable.class, table);
        assertEquals("unnest", table.function().name());
    }

    @Test
    @DisplayName("Function table in FROM clause")
    void functionTableInFromClause() {
        var query = select(col("num"))
            .from(tbl(func("generate_series", arg(lit(1)), arg(lit(10)))).as("series"))
            .build();

        assertNotNull(query);
        assertInstanceOf(FunctionTable.class, query.from());
        assertEquals("series", ((FunctionTable) query.from()).alias());
    }

    @Test
    @DisplayName("Function table with column aliases")
    void functionTableWithColumnAliases() {
        var table = tbl(func("json_each", arg(col("data"))))
            .as("t")
            .columnAliases("key", "value");

        assertInstanceOf(FunctionTable.class, table);
        assertEquals("t", table.alias());
        assertEquals(2, table.columnAliases().size());
    }

    @Test
    @DisplayName("Function table WITH ORDINALITY")
    void functionTableWithOrdinality() {
        var table = tbl(func("generate_series", arg(lit(1)), arg(lit(3))))
            .withOrdinality()
            .as("s")
            .columnAliases("num", "ord");

        assertTrue(table.ordinality());
        assertEquals("s", table.alias());
        assertEquals(2, table.columnAliases().size());
    }

    @Test
    @DisplayName("Lateral wrapping query table")
    void lateralWrappingQueryTable() {
        var subquery = select(col("*")).from(tbl("users")).build();
        var lateral = tbl(subquery).as("sub").lateral();

        assertNotNull(lateral);
        assertInstanceOf(Lateral.class, lateral);
        assertInstanceOf(QueryTable.class, lateral.inner());
    }

    @Test
    @DisplayName("Lateral wrapping function table")
    void lateralWrappingFunctionTable() {
        var func = func("unnest", arg(col("arr")));
        var lateral = tbl(func).as("t").lateral();

        assertNotNull(lateral);
        assertInstanceOf(Lateral.class, lateral);
        assertInstanceOf(FunctionTable.class, lateral.inner());
    }

    @Test
    @DisplayName("Complex query with lateral function table")
    void complexQueryWithLateralFunctionTable() {
        var query = select(col("t", "id"), col("u", "val"))
            .from(tbl("t"))
            .join(inner(
                tbl(func("unnest", arg(col("t", "arr")))).as("u").columnAliases("val").lateral())
                .on(col("t", "id").gt(lit(0)))
            )
            .build();

        assertNotNull(query);
        assertEquals(1, query.joins().size());
        var join = query.joins().getFirst();
        assertInstanceOf(OnJoin.class, join);
        var right = join.right();
        assertInstanceOf(Lateral.class, right);
        assertInstanceOf(FunctionTable.class, ((Lateral) right).inner());
    }

    @Test
    @DisplayName("Multiple function tables in query")
    void multipleFunctionTablesInQuery() {
        var query = select(col("s", "num"), col("u", "val"))
            .from(tbl(func("generate_series", arg(lit(1)), arg(lit(10)))).as("s").columnAliases("num"))
            .join(cross(tbl(func("unnest", arg(array(lit("a"), lit("b"))))).as("u").columnAliases("val")))
            .build();

        assertNotNull(query);
        assertInstanceOf(FunctionTable.class, query.from());
        assertEquals(1, query.joins().size());
        var join = query.joins().getFirst();
        assertInstanceOf(CrossJoin.class, join);
        assertInstanceOf(FunctionTable.class, join.right());
    }

    @Test
    @DisplayName("Lateral in subquery")
    void lateralInSubquery() {
        var innerQuery = select(col("*"))
            .from(tbl(func("generate_series", arg(lit(1)), arg(col("t", "max")))).as("s").lateral())
            .build();
        
        var outerQuery = select(col("*"))
            .from(tbl("t"))
            .join(inner(tbl(innerQuery).as("sub"))
                .on(col("t", "id").eq(col("sub", "id"))));

        assertNotNull(outerQuery);
        var innerFrom = innerQuery.from();
        assertInstanceOf(Lateral.class, innerFrom);
    }
}
