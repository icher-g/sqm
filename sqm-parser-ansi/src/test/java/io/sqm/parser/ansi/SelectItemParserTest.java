package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Objects;

class SelectItemParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final ExprSelectItemParser parser = new ExprSelectItemParser();

    @Test
    @DisplayName("Parses name only")
    void name_only() {
        var r = parser.parse("c1", ctx);
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        ExprSelectItem item = r.value();
        Assertions.assertInstanceOf(ColumnExpr.class, item.expr());
        ColumnExpr n = item.expr().asColumn().orElseThrow();
        Assertions.assertEquals("c1", n.name());
        Assertions.assertNull(item.alias());
        Assertions.assertNull(n.tableAlias());
    }

    @Test
    @DisplayName("Parses table.name with alias via AS")
    void table_name_as_alias() {
        var r = parser.parse("t1.c1 AS a1", ctx);
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        var n = r.value().expr().asColumn().orElseThrow();
        Assertions.assertEquals("t1", n.tableAlias());
        Assertions.assertEquals("c1", n.name());
        Assertions.assertEquals("a1", r.value().alias());
    }

    @Test
    @DisplayName("Named column with bare alias")
    void named_with_bare_alias() {
        var r = parser.parse("t1.c1 a1", ctx);
        Assertions.assertTrue(r.ok());
        var n = r.value().expr().asColumn().orElseThrow();
        Assertions.assertEquals("t1", n.tableAlias());
        Assertions.assertEquals("c1", n.name());
        Assertions.assertEquals("a1", r.value().alias());
    }

    @Test
    @DisplayName("Invalid expr")
    void invalid_expr() {
        var r = parser.parse("(price * qty) AS total", ctx);
        Assertions.assertFalse(r.ok());
    }

    @Test
    @DisplayName("Parses function column with alias")
    void func_with_alias() {
        var r = parser.parse("lower(name) lname", ctx);
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        var f = r.value().expr().asFunc().orElseThrow();
        Assertions.assertEquals("lower", f.name());
        Assertions.assertEquals("lname", r.value().alias());
        Assertions.assertNull(f.distinctArg());
        Assertions.assertEquals(1, f.args().size());
        var a0 = f.args().get(0);
        Assertions.assertInstanceOf(FunctionExpr.Arg.ExprArg.class, a0);
        var c0 = ((FunctionExpr.Arg.ExprArg) a0).expr().asColumn().orElseThrow();
        Assertions.assertNull(c0.tableAlias());
        Assertions.assertEquals("name", c0.name());
    }

    @Test
    @DisplayName("Parses DISTINCT and qualified arg: COUNT(DISTINCT t.id) AS c")
    void func_distinct_and_qualified() {
        var r = parser.parse("COUNT(DISTINCT t.id) AS c", ctx);
        Assertions.assertTrue(r.ok());
        var f = r.value().expr().asFunc().orElseThrow();
        Assertions.assertEquals("COUNT", f.name());
        Assertions.assertTrue(f.distinctArg());
        Assertions.assertEquals("c", r.value().alias());
        Assertions.assertEquals(1, f.args().size());
        var cr = f.args().get(0).asExprArg().orElseThrow().expr().asColumn().orElseThrow();
        Assertions.assertEquals("t", cr.tableAlias());
        Assertions.assertEquals("id", cr.name());
    }

    @Test
    @DisplayName("Parses nested calls and numeric/string literals")
    void func_nested_and_literals() {
        var r = parser.parse("substr(upper(name), 1, '2')", ctx);
        Assertions.assertTrue(r.ok());
        var f = r.value().expr().asFunc().orElseThrow();
        Assertions.assertEquals("substr", f.name());
        Assertions.assertEquals(3, f.args().size());
        Assertions.assertInstanceOf(FunctionExpr.class, f.args().get(0).asExprArg().orElseThrow().expr());
        Assertions.assertInstanceOf(LiteralExpr.class, f.args().get(1).asExprArg().orElseThrow().expr());
        Assertions.assertInstanceOf(LiteralExpr.class, f.args().get(2).asExprArg().orElseThrow().expr());
        var nested = (FunctionExpr) f.args().get(0).asExprArg().orElseThrow().expr();
        Assertions.assertEquals("upper", nested.name());
    }

    @Test
    @DisplayName("Non-CASE input still parsed (NamedColumn path)")
    void non_case_falls_back_to_named_column() {
        var pr = parser.parse("t.col as c", ctx);
        Assertions.assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());
        Assertions.assertInstanceOf(ColumnExpr.class, pr.value().expr());
        var nc = pr.value().expr().asColumn().orElseThrow();
        Assertions.assertEquals("col", nc.name());
        Assertions.assertEquals("t", nc.tableAlias());
        Assertions.assertEquals("c", pr.value().alias());
    }

    @Nested
    @DisplayName("Happy path CASE parsing")
    class Happy {

        @Test
        @DisplayName("CASE with one WHEN/THEN, ELSE literal, alias with AS")
        void case_simple_with_else_and_alias_with_as() {
            var sql = "CASE WHEN t.x > 0 THEN 'pos' ELSE 'neg' END AS sign";
            var pr = parser.parse(sql, ctx);
            Assertions.assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());

            var col = pr.value();
            var cc = col.expr().asCase().orElseThrow();

            Assertions.assertEquals("sign", col.alias());
            Assertions.assertEquals(1, cc.whens().size());

            var arm = cc.whens().get(0);
            Assertions.assertNotNull(arm.when(), "WHEN filter must be parsed");
            Assertions.assertNotNull(arm.then(), "THEN expr must be parsed");
            Assertions.assertInstanceOf(LiteralExpr.class, arm.then(), "THEN 'pos' should be a literal Values");

            Assertions.assertNotNull(cc.elseExpr(), "ELSE must be parsed");
            Assertions.assertInstanceOf(LiteralExpr.class, cc.elseExpr(), "ELSE 'neg' should be a literal Values");
        }

        @Test
        @DisplayName("CASE with multiple WHEN/THEN, no ELSE, bare alias")
        void case_multiple_when_no_else_bare_alias() {
            var sql = """
                CASE
                  WHEN a = 1 THEN 10
                  WHEN a = 2 THEN 20
                END result
                """;
            var pr = parser.parse(sql, ctx);
            Assertions.assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());

            var cc = pr.value().expr().asCase().orElseThrow();
            Assertions.assertEquals("result", pr.value().alias());
            Assertions.assertEquals(2, cc.whens().size());
            Assertions.assertNull(cc.elseExpr(), "ELSE is optional and should be null here");

            Assertions.assertInstanceOf(LiteralExpr.class, cc.whens().get(0).then());
            Assertions.assertInstanceOf(LiteralExpr.class, cc.whens().get(1).then());
        }

        @Test
        @DisplayName("CASE THEN result is a qualified column, alias omitted")
        void case_then_is_qualified_column() {
            var sql = "CASE WHEN flag THEN t2.name END";
            var pr = parser.parse(sql, ctx);
            Assertions.assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());

            var cc = pr.value().expr().asCase().orElseThrow();
            Assertions.assertNull(pr.value().alias());
            Assertions.assertEquals(1, cc.whens().size());
            var thenExpr = cc.whens().get(0).then();
            Assertions.assertInstanceOf(ColumnExpr.class, thenExpr, "THEN t2.name should be parsed as a NamedColumn");
            var nc = thenExpr.asColumn().orElseThrow();
            Assertions.assertEquals("name", nc.name());
            Assertions.assertEquals("t2", nc.tableAlias());
        }

        @Test
        @DisplayName("Nested CASE inside THEN")
        void nested_case_in_then() {
            var sql = """
                CASE WHEN x > 0 THEN
                  CASE WHEN y > 10 THEN 'A' ELSE 'B' END
                ELSE 'C' END alias1
                """;
            var pr = parser.parse(sql, ctx);
            Assertions.assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());

            var outer = pr.value().expr().asCase().orElseThrow();
            Assertions.assertEquals("alias1", pr.value().alias());
            Assertions.assertEquals(1, outer.whens().size());

            var inner = outer.whens().get(0).then();
            Assertions.assertInstanceOf(CaseExpr.class, inner, "THEN should contain a nested CASE");
            var innerCase = inner.asCase().orElseThrow();
            Assertions.assertEquals(1, innerCase.whens().size());
            Assertions.assertNotNull(innerCase.elseExpr());
        }

        @Test
        @DisplayName("CASE with boolean/NULL literals in THEN/ELSE")
        void case_boolean_and_null_literals() {
            var sql = "CASE WHEN active THEN TRUE ELSE NULL END as is_active";
            var pr = parser.parse(sql, ctx);
            Assertions.assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());

            var cc = pr.value().expr().asCase().orElseThrow();
            Assertions.assertEquals("is_active", pr.value().alias());
            Assertions.assertInstanceOf(LiteralExpr.class, cc.whens().get(0).then());
            Assertions.assertInstanceOf(LiteralExpr.class, cc.elseExpr());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("CASE without WHEN → error")
        void case_without_when_errors() {
            var sql = "CASE ELSE 1 END";
            var pr = parser.parse(sql, ctx);
            Assertions.assertFalse(pr.ok());
            Assertions.assertTrue(Objects.requireNonNull(pr.errorMessage()).contains("at least one WHEN"), pr::errorMessage);
        }

        @Test
        @DisplayName("Missing THEN after WHEN predicate → error")
        void missing_then_errors() {
            var sql = "CASE WHEN a = 1  10 ELSE 0 END";
            var pr = parser.parse(sql, ctx);
            Assertions.assertFalse(pr.ok());
            Assertions.assertTrue(Objects.requireNonNull(pr.errorMessage()).toLowerCase().contains("expected then"), pr::errorMessage);
        }

        @Test
        @DisplayName("Missing END → error")
        void missing_end_errors() {
            var sql = "CASE WHEN a = 1 THEN 10";
            var pr = parser.parse(sql, ctx);
            Assertions.assertFalse(pr.ok());
            Assertions.assertTrue(Objects.requireNonNull(pr.errorMessage()).toLowerCase().contains("expected end"), pr::errorMessage);
        }
    }
}