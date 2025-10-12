package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnParserTest {

    private final ColumnParser parser = new ColumnParser();

    @Test
    @DisplayName("Parses name only")
    void name_only() {
        var r = parser.parse("c1");
        assertTrue(r.ok(), () -> "problems: " + r.problems());
        Column c = r.value();
        assertInstanceOf(NamedColumn.class, c);
        NamedColumn n = (NamedColumn) c;
        assertEquals("c1", n.name());
        assertNull(n.alias());
        assertNull(n.table());
    }

    @Test
    @DisplayName("Parses table.name with alias via AS")
    void table_name_as_alias() {
        var r = parser.parse("t1.c1 AS a1");
        assertTrue(r.ok(), () -> "problems: " + r.problems());
        NamedColumn n = (NamedColumn) r.value();
        assertEquals("t1", n.table());
        assertEquals("c1", n.name());
        assertEquals("a1", n.alias());
    }

    @Test
    @DisplayName("Named column with bare alias")
    void named_with_bare_alias() {
        var r = parser.parse("t1.c1 a1");
        assertTrue(r.ok());
        var n = (NamedColumn) r.value();
        assertEquals("t1", n.table());
        assertEquals("c1", n.name());
        assertEquals("a1", n.alias());
    }

    @Test
    @DisplayName("Invalid expr")
    void invalid_expr() {
        var r = parser.parse("(price * qty) AS total");
        assertFalse(r.ok());
    }

    @Test
    @DisplayName("Parses function column with alias")
    void func_with_alias() {
        var r = parser.parse("lower(name) lname");
        assertTrue(r.ok(), () -> "problems: " + r.problems());
        assertInstanceOf(FunctionColumn.class, r.value());
        FunctionColumn f = (FunctionColumn) r.value();
        assertEquals("lower", f.name());
        assertEquals("lname", f.alias());
        assertFalse(f.distinct());
        assertEquals(1, f.args().size());
        var a0 = f.args().get(0);
        assertInstanceOf(FunctionColumn.Arg.Column.class, a0);
        var c0 = (FunctionColumn.Arg.Column) a0;
        assertNull(c0.table());
        assertEquals("name", c0.name());
    }

    @Test
    @DisplayName("Parses DISTINCT and qualified arg: COUNT(DISTINCT t.id) AS c")
    void func_distinct_and_qualified() {
        var r = parser.parse("COUNT(DISTINCT t.id) AS c");
        assertTrue(r.ok());
        var f = (FunctionColumn) r.value();
        assertEquals("COUNT", f.name());
        assertTrue(f.distinct());
        assertEquals("c", f.alias());
        assertEquals(1, f.args().size());
        var cr = (FunctionColumn.Arg.Column) f.args().get(0);
        assertEquals("t", cr.table());
        assertEquals("id", cr.name());
    }

    @Test
    @DisplayName("Parses nested calls and numeric/string literals")
    void func_nested_and_literals() {
        var r = parser.parse("substr(upper(name), 1, '2')");
        assertTrue(r.ok());
        var f = (FunctionColumn) r.value();
        assertEquals("substr", f.name());
        assertEquals(3, f.args().size());
        assertInstanceOf(FunctionColumn.Arg.Function.class, f.args().get(0));
        assertInstanceOf(FunctionColumn.Arg.Literal.class,  f.args().get(1));
        assertInstanceOf(FunctionColumn.Arg.Literal.class,  f.args().get(2));
        var nested = (FunctionColumn.Arg.Function) f.args().get(0);
        assertEquals("upper", nested.call().name());
    }

    @Nested
    @DisplayName("Happy path CASE parsing")
    class Happy {

        @Test
        @DisplayName("CASE with one WHEN/THEN, ELSE literal, alias with AS")
        void case_simple_with_else_and_alias_with_as() {
            var sql = "CASE WHEN t.x > 0 THEN 'pos' ELSE 'neg' END AS sign";
            var pr = parser.parse(sql);
            assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());

            var col = pr.value();
            assertInstanceOf(CaseColumn.class, col);
            var cc = (CaseColumn) col;

            assertEquals("sign", cc.alias());
            assertEquals(1, cc.whens().size());

            var arm = cc.whens().get(0);
            assertNotNull(arm.when(), "WHEN filter must be parsed");
            assertNotNull(arm.then(), "THEN expr must be parsed");
            assertInstanceOf(Values.class, arm.then(), "THEN 'pos' should be a literal Values");

            assertNotNull(cc.elseValue(), "ELSE must be parsed");
            assertInstanceOf(Values.class, cc.elseValue(), "ELSE 'neg' should be a literal Values");
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
            var pr = parser.parse(sql);
            assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());

            var cc = (CaseColumn) pr.value();
            assertEquals("result", cc.alias());
            assertEquals(2, cc.whens().size());
            assertNull(cc.elseValue(), "ELSE is optional and should be null here");

            assertInstanceOf(Values.class, cc.whens().get(0).then());
            assertInstanceOf(Values.class, cc.whens().get(1).then());
        }

        @Test
        @DisplayName("CASE THEN result is a qualified column, alias omitted")
        void case_then_is_qualified_column() {
            var sql = "CASE WHEN flag THEN t2.name END";
            var pr = parser.parse(sql);
            assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());

            var cc = (CaseColumn) pr.value();
            assertNull(cc.alias());
            assertEquals(1, cc.whens().size());
            var thenExpr = cc.whens().get(0).then();
            assertInstanceOf(NamedColumn.class, thenExpr, "THEN t2.name should be parsed as a NamedColumn");
            var nc = (NamedColumn) thenExpr;
            assertEquals("name", nc.name());
            assertEquals("t2", nc.table());
        }

        @Test
        @DisplayName("Nested CASE inside THEN")
        void nested_case_in_then() {
            var sql = """
                      CASE WHEN x > 0 THEN
                        CASE WHEN y > 10 THEN 'A' ELSE 'B' END
                      ELSE 'C' END alias1
                      """;
            var pr = parser.parse(sql);
            assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());

            var outer = (CaseColumn) pr.value();
            assertEquals("alias1", outer.alias());
            assertEquals(1, outer.whens().size());

            var inner = outer.whens().get(0).then();
            assertInstanceOf(CaseColumn.class, inner, "THEN should contain a nested CASE");
            var innerCase = (CaseColumn) inner;
            assertEquals(1, innerCase.whens().size());
            assertNotNull(innerCase.elseValue());
        }

        @Test
        @DisplayName("CASE with boolean/NULL literals in THEN/ELSE")
        void case_boolean_and_null_literals() {
            var sql = "CASE WHEN active THEN TRUE ELSE NULL END as is_active";
            var pr = parser.parse(sql);
            assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());

            var cc = (CaseColumn) pr.value();
            assertEquals("is_active", cc.alias());
            assertInstanceOf(Values.class, cc.whens().get(0).then());
            assertInstanceOf(Values.class, cc.elseValue());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("CASE without WHEN → error")
        void case_without_when_errors() {
            var sql = "CASE ELSE 1 END";
            var pr = parser.parse(sql);
            assertFalse(pr.ok());
            assertTrue(pr.errorMessage().contains("at least one WHEN"), pr::errorMessage);
        }

        @Test
        @DisplayName("Missing THEN after WHEN predicate → error")
        void missing_then_errors() {
            var sql = "CASE WHEN a = 1  10 ELSE 0 END";
            var pr = parser.parse(sql);
            assertFalse(pr.ok());
            assertTrue(pr.errorMessage().toLowerCase().contains("expected then"), pr::errorMessage);
        }

        @Test
        @DisplayName("Missing END → error")
        void missing_end_errors() {
            var sql = "CASE WHEN a = 1 THEN 10";
            var pr = parser.parse(sql);
            assertFalse(pr.ok());
            assertTrue(pr.errorMessage().toLowerCase().contains("expected end"), pr::errorMessage);
        }

        @Test
        @DisplayName("Garbage after parsed CASE alias → parseNamedColumn catches trailing tokens")
        void trailing_tokens_after_alias_errors() {
            var sql = "CASE WHEN a=1 THEN 1 END res EXTRA";
            var pr = parser.parse(sql);
            assertFalse(pr.ok(), "parser should reject unexpected trailing tokens");
        }
    }

    @Test
    @DisplayName("Non-CASE input still parsed (NamedColumn path)")
    void non_case_falls_back_to_named_column() {
        var pr = parser.parse("t.col as c");
        assertTrue(pr.ok(), () -> "parse failed: " + pr.errorMessage());
        assertInstanceOf(NamedColumn.class, pr.value());
        var nc = (NamedColumn) pr.value();
        assertEquals("col", nc.name());
        assertEquals("t", nc.table());
        assertEquals("c", nc.alias());
    }
}