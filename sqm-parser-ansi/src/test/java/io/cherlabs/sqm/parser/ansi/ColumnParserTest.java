package io.cherlabs.sqm.parser.ansi;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.parser.ColumnParser;
import io.cherlabs.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ColumnParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final ColumnParser parser = new ColumnParser();

    @Test
    @DisplayName("Parses name only")
    void name_only() {
        var r = parser.parse("c1", ctx);
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Column c = r.value();
        Assertions.assertInstanceOf(NamedColumn.class, c);
        NamedColumn n = (NamedColumn) c;
        Assertions.assertEquals("c1", n.name());
        Assertions.assertNull(n.alias());
        Assertions.assertNull(n.table());
    }

    @Test
    @DisplayName("Parses table.name with alias via AS")
    void table_name_as_alias() {
        var r = parser.parse("t1.c1 AS a1", ctx);
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        NamedColumn n = (NamedColumn) r.value();
        Assertions.assertEquals("t1", n.table());
        Assertions.assertEquals("c1", n.name());
        Assertions.assertEquals("a1", n.alias());
    }

    @Test
    @DisplayName("Named column with bare alias")
    void named_with_bare_alias() {
        var r = parser.parse("t1.c1 a1", ctx);
        Assertions.assertTrue(r.ok());
        var n = (NamedColumn) r.value();
        Assertions.assertEquals("t1", n.table());
        Assertions.assertEquals("c1", n.name());
        Assertions.assertEquals("a1", n.alias());
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
        Assertions.assertInstanceOf(FunctionColumn.class, r.value());
        FunctionColumn f = (FunctionColumn) r.value();
        Assertions.assertEquals("lower", f.name());
        Assertions.assertEquals("lname", f.alias());
        Assertions.assertFalse(f.distinct());
        Assertions.assertEquals(1, f.args().size());
        var a0 = f.args().get(0);
        Assertions.assertInstanceOf(FunctionColumn.Arg.Column.class, a0);
        var c0 = (FunctionColumn.Arg.Column) a0;
        Assertions.assertNull(c0.table());
        Assertions.assertEquals("name", c0.name());
    }

    @Test
    @DisplayName("Parses DISTINCT and qualified arg: COUNT(DISTINCT t.id) AS c")
    void func_distinct_and_qualified() {
        var r = parser.parse("COUNT(DISTINCT t.id) AS c", ctx);
        Assertions.assertTrue(r.ok());
        var f = (FunctionColumn) r.value();
        Assertions.assertEquals("COUNT", f.name());
        Assertions.assertTrue(f.distinct());
        Assertions.assertEquals("c", f.alias());
        Assertions.assertEquals(1, f.args().size());
        var cr = (FunctionColumn.Arg.Column) f.args().get(0);
        Assertions.assertEquals("t", cr.table());
        Assertions.assertEquals("id", cr.name());
    }

    @Test
    @DisplayName("Parses nested calls and numeric/string literals")
    void func_nested_and_literals() {
        var r = parser.parse("substr(upper(name), 1, '2')", ctx);
        Assertions.assertTrue(r.ok());
        var f = (FunctionColumn) r.value();
        Assertions.assertEquals("substr", f.name());
        Assertions.assertEquals(3, f.args().size());
        Assertions.assertInstanceOf(FunctionColumn.Arg.Function.class, f.args().get(0));
        Assertions.assertInstanceOf(FunctionColumn.Arg.Literal.class, f.args().get(1));
        Assertions.assertInstanceOf(FunctionColumn.Arg.Literal.class, f.args().get(2));
        var nested = (FunctionColumn.Arg.Function) f.args().get(0);
        Assertions.assertEquals("upper", nested.call().name());
    }

    @Test
    @DisplayName("Non-CASE input still parsed (NamedColumn path)")
    void non_case_falls_back_to_named_column() {
        var pr = parser.parse("t.col as c", ctx);
        Assertions.assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());
        Assertions.assertInstanceOf(NamedColumn.class, pr.value());
        var nc = (NamedColumn) pr.value();
        Assertions.assertEquals("col", nc.name());
        Assertions.assertEquals("t", nc.table());
        Assertions.assertEquals("c", nc.alias());
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
            Assertions.assertInstanceOf(CaseColumn.class, col);
            var cc = (CaseColumn) col;

            Assertions.assertEquals("sign", cc.alias());
            Assertions.assertEquals(1, cc.whens().size());

            var arm = cc.whens().get(0);
            Assertions.assertNotNull(arm.when(), "WHEN filter must be parsed");
            Assertions.assertNotNull(arm.then(), "THEN expr must be parsed");
            Assertions.assertInstanceOf(Values.class, arm.then(), "THEN 'pos' should be a literal Values");

            Assertions.assertNotNull(cc.elseValue(), "ELSE must be parsed");
            Assertions.assertInstanceOf(Values.class, cc.elseValue(), "ELSE 'neg' should be a literal Values");
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

            var cc = (CaseColumn) pr.value();
            Assertions.assertEquals("result", cc.alias());
            Assertions.assertEquals(2, cc.whens().size());
            Assertions.assertNull(cc.elseValue(), "ELSE is optional and should be null here");

            Assertions.assertInstanceOf(Values.class, cc.whens().get(0).then());
            Assertions.assertInstanceOf(Values.class, cc.whens().get(1).then());
        }

        @Test
        @DisplayName("CASE THEN result is a qualified column, alias omitted")
        void case_then_is_qualified_column() {
            var sql = "CASE WHEN flag THEN t2.name END";
            var pr = parser.parse(sql, ctx);
            Assertions.assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());

            var cc = (CaseColumn) pr.value();
            Assertions.assertNull(cc.alias());
            Assertions.assertEquals(1, cc.whens().size());
            var thenExpr = cc.whens().get(0).then();
            Assertions.assertInstanceOf(NamedColumn.class, thenExpr, "THEN t2.name should be parsed as a NamedColumn");
            var nc = (NamedColumn) thenExpr;
            Assertions.assertEquals("name", nc.name());
            Assertions.assertEquals("t2", nc.table());
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

            var outer = (CaseColumn) pr.value();
            Assertions.assertEquals("alias1", outer.alias());
            Assertions.assertEquals(1, outer.whens().size());

            var inner = outer.whens().get(0).then();
            Assertions.assertInstanceOf(CaseColumn.class, inner, "THEN should contain a nested CASE");
            var innerCase = (CaseColumn) inner;
            Assertions.assertEquals(1, innerCase.whens().size());
            Assertions.assertNotNull(innerCase.elseValue());
        }

        @Test
        @DisplayName("CASE with boolean/NULL literals in THEN/ELSE")
        void case_boolean_and_null_literals() {
            var sql = "CASE WHEN active THEN TRUE ELSE NULL END as is_active";
            var pr = parser.parse(sql, ctx);
            Assertions.assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());

            var cc = (CaseColumn) pr.value();
            Assertions.assertEquals("is_active", cc.alias());
            Assertions.assertInstanceOf(Values.class, cc.whens().get(0).then());
            Assertions.assertInstanceOf(Values.class, cc.elseValue());
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
            Assertions.assertTrue(pr.errorMessage().contains("at least one WHEN"), pr::errorMessage);
        }

        @Test
        @DisplayName("Missing THEN after WHEN predicate → error")
        void missing_then_errors() {
            var sql = "CASE WHEN a = 1  10 ELSE 0 END";
            var pr = parser.parse(sql, ctx);
            Assertions.assertFalse(pr.ok());
            Assertions.assertTrue(pr.errorMessage().toLowerCase().contains("expected then"), pr::errorMessage);
        }

        @Test
        @DisplayName("Missing END → error")
        void missing_end_errors() {
            var sql = "CASE WHEN a = 1 THEN 10";
            var pr = parser.parse(sql, ctx);
            Assertions.assertFalse(pr.ok());
            Assertions.assertTrue(pr.errorMessage().toLowerCase().contains("expected end"), pr::errorMessage);
        }

        @Test
        @DisplayName("Garbage after parsed CASE alias → parseNamedColumn catches trailing tokens")
        void trailing_tokens_after_alias_errors() {
            var sql = "CASE WHEN a=1 THEN 1 END res EXTRA";
            var pr = parser.parse(sql, ctx);
            Assertions.assertFalse(pr.ok(), "parser should reject unexpected trailing tokens");
        }
    }
}