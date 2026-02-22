package io.sqm.render.ansi;

import io.sqm.core.Node;
import io.sqm.core.SelectQuery;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for QueryTableRenderer.
 */
class QueryTableRendererTest {

    /**
     * Robust comparison not sensitive to newlines/indent.
     */
    private static String flat(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    private static String render(Node node) {
        var ctx = RenderContext.of(new AnsiDialect());
        return ctx.render(node).sql();
    }

    /**
     * Create a minimal Query suitable for nesting as a subquery.
     * Adjust to your real builders/helpers (expr.g., Query.builder()...).
     */
    private SelectQuery makeSimpleQuery() {
        return select(col("u", "id"), col("u", "name"))
            .from(tbl("users").as("u"))
            .build();
    }

    // --- Tests ---------------------------------------------------------------

    @Test
    @DisplayName("Renders subquery with explicit alias: uses AS <alias>")
    void renders_with_explicit_alias() {
        var inner = makeSimpleQuery();
        var qt = tbl(inner).as("users_view"); // explicit alias

        var sql = render(qt);

        // shape contains parentheses and AS alias
        assertTrue(sql.contains("("), "Should open with '(' for subquery");
        assertTrue(sql.contains(")"), "Should close the subquery with ')'");
        assertTrue(flat(sql).contains("AS users_view"), "Should use explicit alias after AS");

        // also ensure inner query text is inside
        assertTrue(flat(sql).contains("SELECT u.id, u.name FROM users AS u"),
            "Should render the inner SELECT");
    }

    @Test
    @DisplayName("No alias when both explicit alias is blank and query.name() is null")
    void no_alias_when_both_missing() {
        var inner = makeSimpleQuery();
        var qt = tbl(inner).as("   "); // blank alias

        var sql = render(qt);

        // Must not contain " AS "
        assertFalse(sql.matches(".*\\sAS\\s+.+"),
            "Must not append AS <alias> when both alias and query name are missing");
    }

    @Test
    @DisplayName("Alias is quoted when needed (expr.g., keyword alias)")
    void alias_is_quoted_if_needed() {
        var inner = makeSimpleQuery();
        // Force fallback via explicit alias that is a keyword (expr.g., SELECT) to check quoting
        var qt = tbl(inner).as("select");

        var sql = render(qt);

        // Depending on your IdentifierQuoter, this could be "AS \"select\"" or [select].
        // We only assert that something was quoted; adapt if your quoter uses a different style.
        assertTrue(
            sql.contains("AS \"select\"") || sql.contains("AS [select]") || sql.contains("AS `select`"),
            "Alias that is a keyword should be quoted by the dialect quoter"
        );
    }

    @Test
    @DisplayName("Keeps subquery on new line with indent and closes before AS")
    void uses_newline_and_indent_block() {
        var inner = makeSimpleQuery();
        var qt = tbl(inner).as("sub");

        var sql = render(qt);

        // Not asserting exact spaces—just structure:
        assertTrue(sql.startsWith("("), "Should start with '('");
        assertTrue(sql.contains("\n"), "Should place the inner query on a new line");
        assertTrue(sql.contains(")"), "Should close the subquery with ')'");
        assertTrue(flat(sql).contains(") AS sub"), "Should place alias after the closing paren");
    }
}
