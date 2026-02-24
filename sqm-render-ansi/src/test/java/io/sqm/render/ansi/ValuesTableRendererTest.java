package io.sqm.render.ansi;

import io.sqm.core.Identifier;
import io.sqm.core.Node;
import io.sqm.core.QuoteStyle;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ValuesTableRenderer.
 */
class ValuesTableRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String render(Node node) {
        return normalize(ctx.render(node).sql());
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("VALUES table without alias")
    void values_table_no_alias() {
        var query = select(lit(1)).from(tbl(rows(row(lit(1), lit("A"))))).build();
        String result = render(query);
        assertTrue(result.contains("VALUES"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("A"));
    }

    @Test
    @DisplayName("VALUES table with alias")
    void values_table_with_alias() {
        var vt = tbl(rows(row(lit(1), lit("A")))).as("v");
        var query = select(lit(1)).from(vt).build();
        String result = render(query);
        assertTrue(result.contains("VALUES"));
        assertTrue(result.contains("AS"));
        assertTrue(result.contains("v"));
    }

    @Test
    @DisplayName("VALUES table with alias and column names")
    void values_table_with_alias_and_columns() {
        var vt = tbl(rows(row(lit(1), lit("Alice")))).as("v").columnAliases("id", "name");
        var query = select(lit(1)).from(vt).build();
        String result = render(query);
        assertTrue(result.contains("VALUES"));
        assertTrue(result.contains("v"));
        assertTrue(result.contains("id"));
        assertTrue(result.contains("name"));
    }

    @Test
    @DisplayName("Multiple rows in VALUES table")
    void values_table_multiple_rows() {
        var vt = tbl(rows(
            row(lit(1), lit("A")),
            row(lit(2), lit("B"))
        )).as("v");
        var query = select(lit(1)).from(vt).build();
        String result = render(query);
        assertTrue(result.contains("VALUES"));
        assertTrue(result.contains("v"));
    }

    @Test
    @DisplayName("VALUES clause is wrapped in parentheses")
    void values_wrapped_in_parentheses() {
        var query = select(lit(1)).from(tbl(rows(row(lit(1), lit("A"))))).build();
        String result = render(query);
        assertTrue(result.contains("(VALUES"));
        assertTrue(result.contains(")"));
    }

    @Test
    @DisplayName("VALUES alias and column aliases preserve supported quotes and fallback unsupported ones")
    void values_table_alias_identifier_quote_preservation_and_fallback() {
        var vt = tbl(rows(row(lit(1), lit("Alice"))))
            .as(Identifier.of("v", QuoteStyle.BACKTICK))
            .columnAliases(java.util.List.of(
                Identifier.of("id", QuoteStyle.DOUBLE_QUOTE),
                Identifier.of("name", QuoteStyle.BRACKETS)
            ));
        var query = select(lit(1)).from(vt).build();
        String result = render(query);

        assertTrue(result.contains("AS \"v\""), "Unsupported backtick alias should fallback to ANSI double quotes");
        assertTrue(result.contains("(\"id\", \"name\")"), "Column aliases should preserve/fallback to ANSI quotes");
    }
}
