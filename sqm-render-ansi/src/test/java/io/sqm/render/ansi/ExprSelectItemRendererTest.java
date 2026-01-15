package io.sqm.render.ansi;

import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for ExprSelectItemRenderer.
 */
class ExprSelectItemRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Assertion failed");
        }
    }

    private String render(Node node) {
        return normalize(ctx.render(node).sql());
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("Simple column without alias")
    void simple_column_no_alias() {
        var query = select(col("id")).from(tbl("users"));
        String result = render(query);
        assertEquals("SELECT id FROM users", result);
    }

    @Test
    @DisplayName("Column with alias")
    void column_with_alias() {
        var query = select(col("id").as("user_id")).from(tbl("users"));
        String result = render(query);
        assertEquals("SELECT id AS user_id FROM users", result);
    }

    @Test
    @DisplayName("Expression with alias")
    void expression_with_alias() {
        var query = select(col("salary").mul(lit(1.1)).as("adjusted_salary")).from(tbl("employees"));
        String result = render(query);
        assertEquals("SELECT salary * 1.1 AS adjusted_salary FROM employees", result);
    }

    @Test
    @DisplayName("Qualified column without alias")
    void qualified_column_no_alias() {
        var query = select(col("u", "name")).from(tbl("users").as("u"));
        String result = render(query);
        assertEquals("SELECT u.name FROM users AS u", result);
    }

    @Test
    @DisplayName("Qualified column with alias")
    void qualified_column_with_alias() {
        var query = select(col("u", "id").as("user_id")).from(tbl("users").as("u"));
        String result = render(query);
        assertEquals("SELECT u.id AS user_id FROM users AS u", result);
    }

    @Test
    @DisplayName("Multiple select items with and without aliases")
    void multiple_items_mixed_aliases() {
        var query = select(
            col("id"),
            col("name").as("full_name"),
            col("email")
        ).from(tbl("users"));
        String result = render(query);
        assertEquals("SELECT id, name AS full_name, email FROM users",
            result.replaceAll("\\s+,\\s+", " , "));
    }

    @Test
    @DisplayName("Aggregate function with alias")
    void aggregate_with_alias() {
        var query = select(col("status"), func("COUNT", arg(col("id"))).as("cnt"))
            .from(tbl("users"))
            .groupBy(group("status"));
        String result = render(query);
        assertTrue(result.contains("COUNT"));
        assertTrue(result.contains("cnt"));
    }
}
