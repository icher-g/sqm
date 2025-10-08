package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.render.DefaultSqlWriter;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.ansi.spi.AnsiRenderContext;
import io.cherlabs.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for CaseColumnRenderer.
 * <p>
 * Assumptions:
 * - Dialect provides renderers for Filter, Values, NamedColumn, FunctionColumn, CaseColumn.
 * - BufferSqlWriter(ctx) is the user's concrete SqlWriter.
 * - Identifiers like "a", "x", "is_active", "name" do NOT require quoting in your IdentifierQuoter.
 * If your quoter chooses to quote them, adapt the expected strings accordingly.
 */
class CaseColumnRendererTest {

    // --- helpers -------------------------------------------------------------

    private RenderContext ctx() {
        return new AnsiRenderContext();
    }

    private SqlWriter writer(RenderContext ctx) {
        return new DefaultSqlWriter(ctx);
    }

    private Column col(String name) {
        return Column.of(name);
    }

    private Column col(String table, String name) {
        return Column.of(name).from(table);
    }

    private Values val(Object v) {
        return Values.single(v);
    }

    private WhenThen arm(Filter when, Entity then) {
        return new WhenThen(when, then);
    }

    private String render(CaseColumn cc) {
        var ctx = ctx();
        var w = writer(ctx);
        new CaseColumnRenderer().render(cc, ctx, w);
        // assuming you don't pass params here
        return w.toText(of()).sql();
    }

    // --- tests ---------------------------------------------------------------

    @Test
    @DisplayName("Single WHEN/THEN with ELSE (TRUE/NULL), alias with AS")
    void single_when_else_boolean_and_null_with_alias() {
        // CASE WHEN a = TRUE THEN TRUE ELSE NULL END AS is_active
        var when = Filter.column(col("a")).eq(true);
        var cc = new CaseColumn(
                of(arm(when, val(true))),
                val(null),
                "is_active"
        );

        var sql = render(cc);
        assertEquals("CASE WHEN a = TRUE THEN TRUE ELSE NULL END AS is_active", sql);
    }

    @Test
    @DisplayName("Multiple WHEN arms, numeric THEN, no ELSE, bare alias")
    void multiple_arms_no_else_bare_alias() {
        // CASE WHEN x = 1 THEN 10 WHEN x = 2 THEN 20 END result
        var w1 = Filter.column(col("x")).eq(1);
        var w2 = Filter.column(col("x")).eq(2);

        var cc = new CaseColumn(
                of(
                        arm(w1, val(10)),
                        arm(w2, val(20))
                ),
                null,
                "result"
        );

        var sql = render(cc);
        assertEquals("CASE WHEN x = 1 THEN 10 WHEN x = 2 THEN 20 END AS result", sql,
                "Renderer emits AS before alias by design; adjust if you render bare alias.");
    }

    @Test
    @DisplayName("THEN result is a qualified column")
    void then_is_qualified_column() {
        // CASE WHEN flag = TRUE THEN t2.name END
        var when = Filter.column(col("flag")).eq(true);

        var cc = new CaseColumn(
                of(arm(when, col("t2", "name"))),
                null,
                null
        );

        var sql = render(cc);
        assertEquals("CASE WHEN flag = TRUE THEN t2.name END", sql);
    }

    @Test
    @DisplayName("Nested CASE in THEN and literal ELSE")
    void nested_case_in_then() {
        // outer: CASE WHEN x > 0 THEN <inner> ELSE 'C' END alias1
        // inner: CASE WHEN y > 10 THEN 'A' ELSE 'B' END
        var inner = new CaseColumn(
                of(
                        arm(
                                Filter.column(col("y")).gt(10),
                                val("A")
                        )
                ),
                val("B"),
                null
        );

        var outer = new CaseColumn(
                of(
                        arm(
                                Filter.column(col("x")).gt(0),
                                inner
                        )
                ),
                val("C"),
                "alias1"
        );

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
        var cc = new CaseColumn(
                of(arm(Filter.column(col("score")).gte(90), val("A"))),
                null,
                "grade"
        );

        var sql = render(cc);
        assertEquals("CASE WHEN score >= 90 THEN 'A' END AS grade", sql);
    }

    @Test
    @DisplayName("Multiple arms with mixed THEN types (column and literal)")
    void mixed_then_types() {
        // CASE WHEN with_name THEN t.name WHEN with_code THEN 'N/A' END label
        var cc = new CaseColumn(
                of(
                        arm(Filter.column(col("with_name")).eq(true), col("t", "name")),
                        arm(Filter.column(col("with_code")).eq(true), val("N/A"))
                ),
                null,
                "label"
        );

        var sql = render(cc);
        assertEquals("CASE WHEN with_name = TRUE THEN t.name WHEN with_code = TRUE THEN 'N/A' END AS label", sql);
    }
}
