package io.sqm.render.ansi;

import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for CaseColumnRenderer.
 */
class CaseExprRendererTest {

    // --- helpers -------------------------------------------------------------

    private String render(Node node) {
        var ctx = RenderContext.of(new AnsiDialect());
        return ctx.render(node).sql();
    }

    // --- tests ---------------------------------------------------------------

    @Test
    @DisplayName("Single WHEN/THEN with ELSE (TRUE/NULL), alias with AS")
    void single_when_else_boolean_and_null_with_alias() {
        // CASE WHEN a = TRUE THEN TRUE ELSE NULL END AS is_active
        var cc = kase(
            when(col("a").eq(true))
                .then(true))
            .elseValue(null)
            .as("is_active");

        var sql = render(cc);
        assertEquals("CASE WHEN a = TRUE THEN TRUE ELSE NULL END AS is_active", sql);
    }

    @Test
    @DisplayName("Multiple WHEN arms, numeric THEN, no ELSE, bare alias")
    void multiple_arms_no_else_bare_alias() {
        // CASE WHEN x = 1 THEN 10 WHEN x = 2 THEN 20 END result
        var w1 = col("x").eq(1);
        var w2 = col("x").eq(2);

        var cc = kase(
            when(w1).then(10),
            when(w2).then(20)
        ).as("result");

        var sql = render(cc);
        assertEquals("CASE WHEN x = 1 THEN 10 WHEN x = 2 THEN 20 END AS result", sql,
            "Renderer emits AS before alias by design; adjust if you render bare alias.");
    }

    @Test
    @DisplayName("THEN result is a qualified column")
    void then_is_qualified_column() {
        // CASE WHEN flag = TRUE THEN t2.name END
        var cc = kase(
            when(col("flag").eq(true))
                .then(col("t2", "name"))
        );

        var sql = render(cc);
        assertEquals("CASE WHEN flag = TRUE THEN t2.name END", sql);
    }

    @Test
    @DisplayName("Nested CASE in THEN and literal ELSE")
    void nested_case_in_then() {
        // outer: CASE WHEN x > 0 THEN <inner> ELSE 'C' END alias1
        // inner: CASE WHEN y > 10 THEN 'A' ELSE 'B' END
        var inner = kase(
            when(col("y").gt(10)).then("A")
        ).elseValue("B");

        var outer = kase(
            when(col("x").gt(0)).then(inner))
            .elseValue("C")
            .as("alias1");

        var sql = render(outer);
        assertEquals(
            "CASE WHEN x > 0 THEN CASE WHEN y > 10 THEN 'A' ELSE 'B' END ELSE 'C' END AS alias1",
            sql
        );
    }

    @Test
    @DisplayName("ELSE omitted")
    void else_omitted() {
        // CASE WHEN score >= 90 THEN 'A' END grade
        var cc = kase(
            when(col("score").gte(90)).then("A"))
            .as("grade");

        var sql = render(cc);
        assertEquals("CASE WHEN score >= 90 THEN 'A' END AS grade", sql);
    }

    @Test
    @DisplayName("Multiple arms with mixed THEN types (column and literal)")
    void mixed_then_types() {
        // CASE WHEN with_name THEN t.name WHEN with_code THEN 'N/A' END label
        var cc = kase(
            when(col("with_name").eq(true)).then(col("t", "name")),
            when(col("with_code").eq(true)).then("N/A"))
            .as("label");

        var sql = render(cc);
        assertEquals("CASE WHEN with_name = TRUE THEN t.name WHEN with_code = TRUE THEN 'N/A' END AS label", sql);
    }
}
