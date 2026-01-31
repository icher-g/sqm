package io.sqm.render.postgresql;

import io.sqm.core.*;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PostgreSQL {@link FunctionTableRenderer}.
 */
@DisplayName("PostgreSQL FunctionTableRenderer Tests")
class FunctionTableRendererTest {

    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        renderContext = RenderContext.of(new PostgresDialect());
    }

    @Test
    @DisplayName("Render simple table function")
    void rendersSimpleTableFunction() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = TableRef.function(func);
        var sql = renderContext.render(table).sql();
        
        assertEquals("generate_series(1, 10)", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render table function with alias")
    void rendersTableFunctionWithAlias() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = TableRef.function(func).as("nums");
        var sql = renderContext.render(table).sql();
        
        assertEquals("generate_series(1, 10) AS nums", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render table function with column aliases")
    void rendersTableFunctionWithColumnAliases() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = TableRef.function(func).as("t").columnAliases(List.of("n"));
        var sql = renderContext.render(table).sql();
        
        assertEquals("generate_series(1, 10) AS t(n)", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render table function WITH ORDINALITY")
    void rendersTableFunctionWithOrdinality() {
        var func = func("generate_series", arg(lit(1)), arg(lit(3)));
        var table = TableRef.function(func)
            .withOrdinality()
            .as("t")
            .columnAliases(List.of("n", "ord"));
        var sql = renderContext.render(table).sql();

        assertEquals("generate_series(1, 3) WITH ORDINALITY AS t(n, ord)", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render unnest function")
    void rendersUnnestFunction() {
        var arr = array(lit(1), lit(2), lit(3));
        var func = func("unnest", arg(arr));
        var table = TableRef.function(func);
        var sql = renderContext.render(table).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("unnest(ARRAY[1, 2, 3])"));
    }

    @Test
    @DisplayName("Render json_array_elements function")
    void rendersJsonArrayElementsFunction() {
        var cast = CastExpr.of(lit("[1,2,3]"), type("json"));
        var func = func("json_array_elements", arg(cast));
        var table = TableRef.function(func);
        var sql = renderContext.render(table).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("json_array_elements"));
    }

    @Test
    @DisplayName("Render table function in FROM clause")
    void rendersTableFunctionInFromClause() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var query = select(col("*"))
            .from(TableRef.function(func).as("s").columnAliases(List.of("n")));
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("FROM generate_series(1, 10) AS s(n)"));
    }

    @Test
    @DisplayName("Render table function in JOIN")
    void rendersTableFunctionInJoin() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var query = select(col("*"))
            .from(tbl("users").as("u"))
            .join(inner(TableRef.function(func).as("s").columnAliases(List.of("n")))
                .on(col("u", "id").eq(col("s", "n"))));
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("JOIN generate_series(1, 10) AS s(n)"));
    }

    @Test
    @DisplayName("Render multiple table functions")
    void rendersMultipleTableFunctions() {
        var func1 = func("generate_series", arg(lit(1)), arg(lit(5)));
        var func2 = func("generate_series", arg(lit(1)), arg(lit(3)));
        
        var query = select(col("*"))
            .from(TableRef.function(func1).as("s1"))
            .join(cross(TableRef.function(func2).as("s2")));
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("generate_series(1, 5) AS s1"));
        assertTrue(normalizeWhitespace(sql).contains("generate_series(1, 3) AS s2"));
    }

    @Test
    @DisplayName("Render table function with column reference in argument")
    void rendersTableFunctionWithColumnArg() {
        var func = func("unnest", arg(col("u", "tags")));
        var query = select(star())
            .from(tbl("users").as("u"))
            .join(cross(TableRef.function(func).as("t").columnAliases(List.of("tag"))));
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("unnest(u.tags) AS t(tag)"));
    }

    @Test
    @DisplayName("Render table function with multiple column aliases")
    void rendersTableFunctionWithMultipleColumnAliases() {
        var func = func("json_to_recordset", arg(col("data")));
        var table = TableRef.function(func).as("t").columnAliases(List.of("id", "name", "email"));
        var sql = renderContext.render(table).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("AS t(id, name, email)"));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
