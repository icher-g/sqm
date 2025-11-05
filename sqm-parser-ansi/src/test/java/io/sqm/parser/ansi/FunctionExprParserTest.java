package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FunctionExprParserTest {

    static FunctionExpr parseFunc(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(FunctionExpr.class, sql);
        assertFalse(result.isError(), "Parser failed");
        assertNotNull(result.value(), "Expected FunctionExpr as top-level");
        return result.value();
    }

    @Test
    void parses_lower_with_case_argument_single_when() {
        var f = parseFunc("LOWER(CASE WHEN u.flag > 0 THEN u.name END)");

        assertEquals("lower", f.name().toLowerCase());
        assertEquals(1, f.args().size());

        var a0 = f.args().get(0);
        assertInstanceOf(FunctionExpr.Arg.ExprArg.class, a0);
        var expr = ((FunctionExpr.Arg.ExprArg) a0).expr();
        assertInstanceOf(CaseExpr.class, expr);
        var c = (CaseExpr) expr;

        assertEquals(1, c.whens().size());
        WhenThen wt = c.whens().get(0);
        assertNotNull(wt.when());
        assertNotNull(wt.then());
        assertNull(c.elseExpr());
    }

    @Test
    void parses_case_with_else_in_function_arg() {
        var f = parseFunc("UPPER(CASE WHEN a > 1 THEN b ELSE 'x' END)");

        var c = (CaseExpr) ((FunctionExpr.Arg.ExprArg) f.args().get(0)).expr();
        assertEquals(1, c.whens().size());
        assertNotNull(c.elseExpr());
    }

    @Test
    void parses_nested_function_and_literal() {
        var f = parseFunc("COALESCE(LOWER(u.name), 'N/A')");

        assertEquals(2, f.args().size());

        var nested = (FunctionExpr) ((FunctionExpr.Arg.ExprArg) f.args().get(0)).expr();
        assertEquals("lower", nested.name().toLowerCase());
        var nestedArg = ((FunctionExpr.Arg.ExprArg) nested.args().get(0)).expr();
        assertInstanceOf(ColumnExpr.class, nestedArg);

        var lit = ((FunctionExpr.Arg.ExprArg) f.args().get(1)).expr();
        assertInstanceOf(LiteralExpr.class, lit);
    }

    @Test
    void parses_coalesce_with_scalar_subquery_argument() {
        var f = parseFunc("COALESCE((SELECT MAX(t.v) FROM t), 0)");

        assertEquals("coalesce", f.name().toLowerCase());
        assertEquals(2, f.args().size());

        var a0 = f.args().get(0);
        assertInstanceOf(FunctionExpr.Arg.ExprArg.class, a0);
        var expr = ((FunctionExpr.Arg.ExprArg) a0).expr();
        assertInstanceOf(QueryExpr.class, expr);

        var q = (QueryExpr) expr;
        assertInstanceOf(SelectQuery.class, q.subquery());
    }

    @Test
    void parses_count_star() {
        var f = parseFunc("COUNT(*)");
        assertEquals("count", f.name().toLowerCase());
        assertEquals(1, f.args().size());
        assertInstanceOf(FunctionExpr.Arg.StarArg.class, f.args().get(0));
    }
}