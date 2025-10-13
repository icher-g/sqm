package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Entity;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.QueryTable;
import io.cherlabs.sqm.render.DefaultRenderContext;
import io.cherlabs.sqm.render.DefaultSqlWriter;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.ansi.spi.AnsiDialect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.cherlabs.sqm.dsl.Dsl.query;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for QueryTableRenderer.
 */
class QueryTableRendererTest {

    // If you already have these helpers in your tests, use them instead.
    private static io.cherlabs.sqm.core.Column col(String alias, String name) {
        return io.cherlabs.sqm.core.Column.of(name).from(alias);
    }

    private static io.cherlabs.sqm.core.NamedTable table(String name) {
        return io.cherlabs.sqm.core.Table.of(name);
    }

    /**
     * Robust comparison not sensitive to newlines/indent.
     */
    private static String flat(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    private static String render(Entity entity) {
        SqlWriter w = new DefaultSqlWriter(new DefaultRenderContext(new AnsiDialect()));
        w.append(entity);
        return w.toText(List.of()).sql();
    }

    /**
     * Create a minimal Query suitable for nesting as a subquery.
     * Adjust to your real builders/helpers (e.g., Query.builder()...).
     */
    private Query makeSimpleQuery() {
        return query()
            .select(col("u", "id"), col("u", "name"))
            .from(table("users").as("u"));
    }

    // --- Tests ---------------------------------------------------------------

    @Test
    @DisplayName("Renders subquery with explicit alias: uses AS <alias>")
    void renders_with_explicit_alias() {
        var inner = makeSimpleQuery();
        var qt = new QueryTable(inner, "users_view"); // explicit alias

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
        var qt = new QueryTable(inner, "   "); // blank alias

        var sql = render(qt);

        // Must not contain " AS "
        assertFalse(sql.matches(".*\\sAS\\s+.+"),
            "Must not append AS <alias> when both alias and query name are missing");
    }

    @Test
    @DisplayName("Alias is quoted when needed (e.g., keyword alias)")
    void alias_is_quoted_if_needed() {
        var inner = makeSimpleQuery();
        // Force fallback via explicit alias that is a keyword (e.g., SELECT) to check quoting
        var qt = new QueryTable(inner, "select");

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
        var qt = new QueryTable(inner, "sub");

        var sql = render(qt);

        // Not asserting exact spaces—just structure:
        assertTrue(sql.startsWith("("), "Should start with '('");
        assertTrue(sql.contains("\n"), "Should place the inner query on a new line");
        assertTrue(sql.contains(")"), "Should close the subquery with ')'");
        assertTrue(flat(sql).contains(") AS sub"), "Should place alias after the closing paren");
    }
}
